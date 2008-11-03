//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.jdunrar;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.SubConfiguration;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.utils.DynByteBuffer;
import jd.utils.Executer;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.ProcessListener;

/**
 * Die klasse dient zum verpacken der Unrar binary.
 * 
 * @author coalado
 * 
 */
public class UnrarWrapper extends Thread {

    private static final int CANNOT_FIND_VOLUME = 1;
    private static final int COULD_NOT_FIND_PASSWORD = 1 << 1;
    private static final int OPENED_SUCCESSFULL = 1 << 2;
    private static final int STARTED = 1 << 3;
    private static final int NO_FILES_FOUND = 1 << 4;
    private static final int FAILED = 1 << 5;
    @SuppressWarnings("unused")
    private static final int FAILED_CRC = 1 << 6;
    private ArrayList<UnrarListener> listener = new ArrayList<UnrarListener>();
    private DownloadLink link;
    private String unrarCommand;
    private String[] passwordList;
    private File file;
    private int status;
    private String password;
    private boolean isProtected = false;
    private ArrayList<ArchivFile> files;

    private boolean overwriteFiles = false;

    private long totalSize;
    private ArchivFile currentlyWorkingOn;

    public ArchivFile getCurrentlyWorkingOn() {
        return currentlyWorkingOn;
    }

    public void setCurrentlyWorkingOn(ArchivFile currentlyWorkingOn) {
        this.currentlyWorkingOn = currentlyWorkingOn;
    }

    private String latestStatus;
    private int currentVolume;
    private long startTime;
    private SubConfiguration config = JDUtilities.getSubConfig(JDLocale.L("plugins.optional.jdunrar.name", "JD-Unrar"));
    private long speed = config.getIntegerProperty("SPEED", 10000000);
    private boolean exactProgress = false;
    private int volumeNum = 1;
    private Exception exception;
    private File extractTo;
    private boolean removeAfterExtraction;

    private ArrayList<String> archiveParts;

    public UnrarWrapper(DownloadLink link) {
        this.link = link;

        if (link == null) { throw new IllegalArgumentException("link==null"); }
        this.file = new File(link.getFileOutput());
        archiveParts = new ArrayList<String>();
    }

    public UnrarWrapper(DownloadLink link, File file) {
        this.link = link;
        if (link == null) { throw new IllegalArgumentException("link==null"); }
        this.file = file;
        archiveParts = new ArrayList<String>();
    }

    public void addUnrarListener(UnrarListener listener) {
        this.removeUnrarListener(listener);
        this.listener.add(listener);

    }

    private void removeUnrarListener(UnrarListener listener) {
        this.listener.remove(listener);

    }

    public ArrayList<ArchivFile> getFiles() {
        return files;
    }

    public void run() {
        try {
            fireEvent(JDUnrarConstants.WRAPPER_STARTED);
            this.status = (STARTED);
            open();
            if (this.status == OPENED_SUCCESSFULL) {
                if (this.files.size() == 0) {
                    this.fireEvent(NO_FILES_FOUND);
                }
                if (this.isProtected && this.password == null) {
                    fireEvent(JDUnrarConstants.WRAPPER_CRACK_PASSWORD);

                    if (this.isProtected && this.password == null) {
                        crackPassword();
                        if (password == null) {
                            fireEvent(JDUnrarConstants.WRAPPER_FAILED_PASSWORD);
                            if (password != null) {
                                String[] tmp = passwordList;
                                this.passwordList = new String[] { password };
                                password = null;
                                crackPassword();
                                passwordList = tmp;
                            }
                            if (password != null) {
                                fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_FOUND);
                            }

                        } else {
                            fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_FOUND);

                        }

                        if (password == null) {
                            this.status = FAILED;
                            fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                            return;
                        }

                    } else {
                        fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_FOUND);
                    }
                }

                this.extract();

                if (this.status == STARTED && this.checkSizes()) {
                    if (removeAfterExtraction) {
                        removeArchiveFiles();
                    }
                    fireEvent(JDUnrarConstants.WRAPPER_FINISHED_SUCCESSFULL);
                } else {
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                }

            } else {
                this.fireEvent(this.status);
                fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                return;
            }
        } catch (Exception e) {
            this.exception = e;
            e.printStackTrace();
            fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
        }

    }

    private boolean checkSizes() {
        boolean c = true;
        for (ArchivFile f : files) {
            if (f.getSize() != f.getFile().length()) {
                if (!f.getFile().isDirectory()) {
                    c = false;
                } else {
                    f.setPercent(100);
                }
            } else {
                f.setPercent(100);
            }
        }
        return c;

    }

    private void removeArchiveFiles() {
        for (String file : archiveParts) {
            if (file != null && file.trim().length() > 0) {
                if (new File(file).isAbsolute()) {
                    new File(file).delete();
                    new File(file).deleteOnExit();
                    JDUtilities.getLogger().warning("Deleted archive after extraction: " + new File(file));
                } else {
                    new File(this.file.getParentFile(), file).delete();
                    new File(this.file.getParentFile(), file).deleteOnExit();
                    JDUtilities.getLogger().warning("Deleted archive after extraction: " + new File(this.file.getParentFile(), file));
                }
            }
        }

    }

    public Exception getException() {
        return exception;
    }

    public int getStatus() {
        return status;
    }

    private void extract() {

        fireEvent(JDUnrarConstants.WRAPPER_START_EXTRACTION);
        Executer exec = new Executer(unrarCommand);
        exec.addParameter("x");

        exec.addParameter("-p");

        if (overwriteFiles) {
            exec.addParameter("-o+");
        } else {
            exec.addParameter("-o-");
        }
        exec.addParameter("-c-");
        exec.addParameter("-v");
        exec.addParameter("-ierr");
        exec.addParameter(file.getAbsolutePath());
        if (extractTo != null) {
            extractTo.mkdirs();
            exec.setRunin(extractTo.getAbsolutePath());
        } else {
            exec.setRunin(file.getParentFile().getAbsolutePath());
        }
        exec.setWaitTimeout(-1);
        exec.addProcessListener(new ExtractListener(), Executer.LISTENER_ERRORSTREAM|Executer.LISTENER_ERRORSTREAM);
        exec.addProcessListener(new PasswordListener(password), Executer.LISTENER_ERRORSTREAM);
        this.status = STARTED;
        exec.start();
        this.startTime = System.currentTimeMillis();
        Thread inter = new Thread() {
            public void run() {
                while (true) {
                    if (!exactProgress) {
                        if (!exactProgress) {
                            long est = speed * ((System.currentTimeMillis() - startTime) / 1000);

                            for (ArchivFile f : files) {
                                long part = Math.min(est, f.getSize());
                                est -= part;
                                if (part == 0 && f.getSize() == 0) {
                                    f.setPercent(100);
                                } else {
                                    f.setPercent((int) ((part * 100) / f.getSize()));
                                }
                                if (est <= 0) break;
                            }
                            fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);

                        } else {
                            return;
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        inter.start();

        exec.waitTimeout();
        inter.interrupt();
        config.setProperty("SPEED", speed);
        config.save();
    }

    private String escapePassword(String password) {
        if (password == null) return password;
        String retpw = "";
        for (int i = 0; i < password.length(); i++) {
            char cur = password.charAt(i);
            if (cur == '"') {
                retpw += '\"';
            } else {
                retpw += (char) cur;
            }
        }
        return retpw;

    }

    private void crackPassword() {
        ArchivFile file = null;
        // suche kleines passwortgeschützte Datei
        for (ArchivFile f : files) {
            if (f.isProtected()) {
                if (file == null) {
                    file = f;
                    continue;
                } else if (f.getSize() < file.getSize()) {
                    file = f;
                }
            }
        }

        File fileFile = JDUtilities.getResourceFile("tmp/unrar/" + System.currentTimeMillis() + ".unrartmp");
        JDUtilities.writeLocalFile(fileFile, file.getFilepath());

        if (file.getSize() < 2097152) {

            for (String pass : this.passwordList) {
                pass = escapePassword(pass);
                Executer exec = new Executer(unrarCommand);
                exec.addParameter("t");
                // exec.addParameter("-p");
                exec.addParameter("-n@" + fileFile.getAbsolutePath());
                exec.addParameter("-c-");
                exec.addParameter(this.file.getName());
                exec.setRunin(this.file.getParentFile().getAbsolutePath());
                exec.setWaitTimeout(-1);

                exec.addProcessListener(new PasswordListener(pass), Executer.LISTENER_ERRORSTREAM);
                exec.start();
                exec.waitTimeout();
                String res = exec.getOutputStream() + " \r\n " + exec.getErrorStream();
                if (res.indexOf(" (password incorrect ?)") != -1) {
                    continue;
                } else if (res.matches("(?s).*[\\s]+All OK[\\s].*")) {
                    this.password = pass;
                    return;
                } else {
                    continue;
                }
            }
        } else {
            for (String pass : this.passwordList) {
                Executer exec = new Executer(unrarCommand);
                exec.addParameter("p");
                // exec.addParameter("-p");
                exec.addParameter("-n@" + fileFile.getAbsolutePath());

                exec.addParameter("-c-");
                exec.addParameter("-ierr");
                exec.addParameter(this.file.getName());
                exec.setRunin(this.file.getParentFile().getAbsolutePath());
                exec.setWaitTimeout(-1);
                exec.addProcessListener(new PasswordListener(pass), Executer.LISTENER_ERRORSTREAM);

                exec.addProcessListener(new ProcessListener() {

                    @Override
                    public void onBufferChanged(Executer exec, DynByteBuffer buffer, int latestNum) {
                        if (buffer.position() >= 50) {
                            exec.interrupt();
                            System.out.println("loaded enough.... interrupt");

                        }

                    }

                    @Override
                    public void onProcess(Executer exec, String latestLine, DynByteBuffer sb) {
                        // TODO Auto-generated method stub

                    }

                }, Executer.LISTENER_STDSTREAM);
                exec.start();
                // Wartet bis der process fertig ist,oder bis er abgebrochen
                // wurde
                exec.waitTimeout();
                if (new Regex(exec.getErrorStream(), "(CRC failed|Total errors: )").matches()) {
                    continue;
                }
                String sig = "";
                DynByteBuffer buff = exec.getInputStreamBuffer();
                buff.flip();
                for (int i = 0; i < buff.limit(); i++) {
                    byte f = buff.get();
                    String s = Integer.toHexString((int) f);
                    s = (s.length() < 2 ? "0" + s : s);
                    s = s.substring(s.length() - 2);
                    sig += s;
                }

                JDUtilities.getLogger().finest(exec.getInputStreamBuffer() + " : " + sig);
                if (sig.trim().length() < 8) continue;
                Signature signature = FileSignatures.getSignature(sig);

                if (signature != null) {
                    if (signature.getExtension().matcher(file.getFilepath()).matches()) {
                        // signatur passt zur extension
                        this.password = pass;
                        return;
                    } else {
                        // signatur passt nicht zur extension.... Es wird
                        // weitergesucht.

                        if (!signature.getDesc().equals("Plaintext")) {
                            this.password = pass;
                        } else {
                            if (password == null) password = pass;
                        }

                    }
                }

            }

        }

    }

    void fireEvent(int status2) {
        for (UnrarListener listener : this.listener) {
            listener.onUnrarEvent(status2, this);
        }

    }

    /**
     * Öffnet das rararchiv und liest die files ein
     * 
     * @throws UnrarException
     */
    private void open() throws UnrarException {
        String pass = null;
        int i = 0;
        fireEvent(JDUnrarConstants.WRAPPER_START_OPEN_ARCHIVE);
        while (true) {
            Executer exec = new Executer(unrarCommand);
            if (i > 0) {
                if (passwordList.length < i) {
                    this.status = COULD_NOT_FIND_PASSWORD;
                    return;
                }
                pass = this.passwordList[i - 1];
            }
            i++;
            exec.addParameter("v");
            exec.addParameter("-p");

            exec.addProcessListener(new PasswordListener(pass), Executer.LISTENER_ERRORSTREAM);
            exec.addParameter("-v");
            exec.addParameter("-c-");
            exec.addParameter(file.getName());
            exec.setRunin(file.getParentFile().getAbsolutePath());
            exec.setWaitTimeout(-1);
            exec.start();
            exec.waitTimeout();
            String res = exec.getOutputStream() + " \r\n " + exec.getErrorStream();
            JDUtilities.getLogger().finest(res);
            if (res.contains("Cannot open ") || res.contains("Das System kann die angegebene Datei nicht finden")) { throw new UnrarException("File not found " + file.getAbsolutePath()); }
            if (res.indexOf(" (password incorrect ?)") != -1) {
                JDUtilities.getLogger().finest("Password incorrect: " + file.getName() + " pw: " + pass);
                continue;
            } else {
                this.status = OPENED_SUCCESSFULL;
                fireEvent(JDUnrarConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS);
                String[] volumes = Pattern.compile("Volume (.*?)Pathname/Comment", Pattern.DOTALL).split(res);
                ArchivFile tmp = null;
                String namen = "";
                this.files = new ArrayList<ArchivFile>();
                this.totalSize = 0;
                for (String volume : volumes) {
                    res = volume;

                    Pattern patternvolumes = Pattern.compile("(.+)\\s*?([\\d]+).*?[\\d]+\\-[\\d]+\\-[\\d]+.*?[\\d]+:[\\d]+.*?(.{1})(.{1})(.{1})", Pattern.CASE_INSENSITIVE);
                    Matcher matchervolumes = patternvolumes.matcher(res);

                    String vol = new Regex(res, "       volume (\\d+)").getMatch(0);
                    if (vol != null) {
                        volumeNum = Integer.parseInt(vol.trim());
                    }
                    while (matchervolumes.find()) {

                        String name = matchervolumes.group(1);
                        if (name.matches("\\*.*")) {
                            name = name.replaceFirst(".", "");
                            long size = Long.parseLong(matchervolumes.group(2));
                            this.isProtected = true;
                            if (pass != null && password != pass) {

                                this.password = pass;
                                fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_FOUND);
                            }
                            if (!name.equals(namen) && !matchervolumes.group(4).equals("D")) {
                                tmp = new ArchivFile(name);
                                tmp.setSize(size);
                                tmp.setPath(this.getExtractTo());
                                tmp.setProtected(true);
                                tmp.addVolume(vol);
                                files.add(tmp);
                                namen = name;
                                totalSize += size;

                            } else if (name.equals(namen)) {
                                tmp.addVolume(vol);
                            }

                        } else {
                            name = name.replaceFirst(".", "");
                            if (!name.equals(namen) && !matchervolumes.group(4).equals("D")) {

                                tmp = new ArchivFile(name);
                                tmp.setPath(this.getExtractTo());
                                long size;
                                tmp.setSize(size = Long.parseLong(matchervolumes.group(2)));
                                totalSize += size;
                                tmp.setProtected(false);
                                tmp.addVolume(vol);
                                files.add(tmp);
                                namen = name;

                            } else if (name.equals(namen)) {
                                tmp.addVolume(vol);
                            }

                        }
                    }

                }
                if (res.indexOf("Cannot find volume") != -1) {
                    this.status = CANNOT_FIND_VOLUME;
                    return;
                }
                return;
            }

        }
    }

    /**
     * Setzt den Pfad zur unrar.exe
     * 
     * @param file
     */
    public void setUnrarCommand(String file) {
        this.unrarCommand = file;

    }

    public void setPasswordList(String[] passwordStringtoArray) {
        this.passwordList = passwordStringtoArray;

    }

    // class UnrarProcessListener extends ProcessListener {
    //
    // private UnrarWrapper uw;
    // private String password = null;
    //
    // public UnrarProcessListener(UnrarWrapper uw, String password) {
    // this.uw = uw;
    // this.password = password;
    // this.interruptafter = -1;
    // }
    //
    // public UnrarProcessListener(UnrarWrapper uw) {
    // this.uw = uw;
    // this.password = null;
    // this.interruptafter = -1;
    // }
    //
    // @Override
    // public void onBufferChanged(Executer exec, DynByteBuffer buffer,
    // DynByteBuffer origin) {
    // if (new Regex(buffer.toString(), ".*?password.*: ").matches()) {
    // debugmsg(buffer.toString());
    // exec.writetoOutputStream(this.password);
    // } else if (new Regex(buffer.toString(),
    // ".*?current.*?password.*?ll ").matches()) {
    // debugmsg(buffer.toString());
    // exec.writetoOutputStream("A");
    // } else if (interruptafter > 0 && origin == exec.getInputStreamBuffer()) {
    // if (origin.position() >= interruptafter) {
    // exec.interrupt();
    // }
    // }
    // }
    //
    // @Override
    // public void onProcess(Executer exec, String latestLine, DynByteBuffer
    // buffer) {
    // debugmsg(latestLine);
    // String match = null;
    // if (latestLine.length() > 0) {
    // // Neue Datei wurde angefangen
    // if ((match = new Regex(latestLine, "Extracting  (.*)").getMatch(0)) !=
    // null) {
    // String currentWorkingFile = match.trim();
    // uw.currentlyWorkingOn = getArchivFile(currentWorkingFile);
    // uw.fireEvent(JDUnrarConstants.WRAPPER_PROGRESS_NEW_SINGLE_FILE_STARTED);
    // }
    // if ((match = new Regex(latestLine, "Extracting from(.*)").getMatch(0)) !=
    // null) {
    // archiveParts.add(match.trim());
    // }
    // if ((match = new Regex(latestLine,
    // "Extracting from.*part(\\d+)\\.").getMatch(0)) != null) {
    // uw.currentVolume = Integer.parseInt(match.trim());
    // long ext = uw.totalSize / uw.volumeNum * (currentVolume - 1);
    // if (ext == 0) { return; }
    // try {
    // uw.speed = ext / ((System.currentTimeMillis() - uw.startTime) / 1000);
    // } catch (Exception e) {
    // }
    // }
    // // ruft die prozentangaben der aktuellen datei
    // if ((match = new Regex(latestLine, "(\\d+)\\%").getMatch(0)) != null) {
    // uw.exactProgress = true;
    // currentlyWorkingOn.setPercent(Integer.parseInt(match));
    // uw.fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);
    // }
    //
    // // datei ok
    // if ((match = new Regex(latestLine, "^\\s*?(OK)").getMatch(0)) != null) {
    // currentlyWorkingOn.setPercent(100);
    // uw.fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);
    // uw.fireEvent(JDUnrarConstants.WRAPPER_PROGRESS_SINGLE_FILE_FINISHED);
    // }
    //
    // if ((match = new Regex(latestLine, "Bad archive (.*)").getMatch(0)) !=
    // null) {
    // uw.status = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC;
    // debugmsg("Bad archive. Prop. CRC error in " + match);
    // exec.interrupt();
    // }
    //
    // if ((match = new Regex(latestLine,
    // "CRC failed in (.*?) \\(").getMatch(0)) != null) {
    // uw.status = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC;
    // exec.interrupt();
    // }
    //
    // }
    //
    // }
    //
    // }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public String getLatestStatus() {
        return latestStatus;
    }

    public long getExtractedSize() {
        long size = 0;
        for (ArchivFile af : files) {
            size += af.getSize() * ((double) af.getPercent() / 100.0);
        }
        return size;
    }

    /**
     * Gibt das ArchiveFile zum lokalen Pfad currentWorkingFile zurück
     * 
     * @param currentWorkingFile
     * @return
     */
    ArchivFile getArchivFile(String currentWorkingFile) {
        for (ArchivFile af : files) {
            if (af.getFilepath().equals(currentWorkingFile)) { return af; }
        }
        return null;
    }

    public String getPassword() {
        return password;
    }

    public long getTotalSize() {
        return this.totalSize;
    }

    public ArchivFile getCurrentFile() {
        return this.currentlyWorkingOn;
    }

    public File getFile() {
        return file;
    }

    public DownloadLink getDownloadLink() {
        return this.link;
    }

    public void setExtractTo(File dl) {

        this.extractTo = dl;

    }

    public File getExtractTo() {
        return extractTo;
    }

    public void setRemoveAfterExtract(boolean setProperty) {
        this.removeAfterExtraction = setProperty;

    }

    public void setOverwrite(boolean booleanProperty) {
        this.overwriteFiles = booleanProperty;

    }

    public void setPassword(String pass) {
        this.password = pass;

    }

    public class ExtractListener extends ProcessListener {

        @Override
        public void onBufferChanged(Executer exec, DynByteBuffer totalBuffer, int latestReadNum) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProcess(Executer exec, String latestLine, DynByteBuffer totalBuffer) {

            String match = null;
            if (latestLine.length() > 0) {
                // Neue Datei wurde angefangen
                if ((match = new Regex(latestLine, "Extracting  (.*)").getMatch(0)) != null) {
                    String currentWorkingFile = match.trim();
                    currentlyWorkingOn = getArchivFile(currentWorkingFile);
                    fireEvent(JDUnrarConstants.WRAPPER_PROGRESS_NEW_SINGLE_FILE_STARTED);
                }
                if ((match = new Regex(latestLine, "Extracting from(.*)").getMatch(0)) != null) {
                    archiveParts.add(match.trim());
                }
                if ((match = new Regex(latestLine, "Extracting from.*part(\\d+)\\.").getMatch(0)) != null) {
                    currentVolume = Integer.parseInt(match.trim());
                    long ext = totalSize / volumeNum * (currentVolume - 1);
                    if (ext == 0) { return; }
                    try {
                        speed = ext / ((System.currentTimeMillis() - startTime) / 1000);
                    } catch (Exception e) {
                    }
                }
                // ruft die prozentangaben der aktuellen datei
                if ((match = new Regex(latestLine, "(\\d+)\\%").getMatch(0)) != null) {
                    exactProgress = true;
                    currentlyWorkingOn.setPercent(Integer.parseInt(match));
                    fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);
                }

                // datei ok
                if ((match = new Regex(latestLine, "^\\s*?(OK)").getMatch(0)) != null) {
                    currentlyWorkingOn.setPercent(100);
                    fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);
                    fireEvent(JDUnrarConstants.WRAPPER_PROGRESS_SINGLE_FILE_FINISHED);
                }

                if ((match = new Regex(latestLine, "Bad archive (.*)").getMatch(0)) != null) {
                    status = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC;

                    exec.interrupt();
                }

                if ((match = new Regex(latestLine, "CRC failed in (.*?) \\(").getMatch(0)) != null) {
                    status = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC;
                    exec.interrupt();
                }

            }
        }

    }
}
