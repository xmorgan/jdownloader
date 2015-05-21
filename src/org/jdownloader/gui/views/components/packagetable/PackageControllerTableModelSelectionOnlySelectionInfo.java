package org.jdownloader.gui.views.components.packagetable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.event.queue.Queue;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData.PackageControllerTableModelDataPackage;

public class PackageControllerTableModelSelectionOnlySelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionInfo<PackageType, ChildrenType> {

    private final PackageControllerTableModel<PackageType, ChildrenType>     tableModel;
    private final PackageControllerTableModelData<PackageType, ChildrenType> tableModelData;
    private ListSelectionModel                                               selectionModel = null;
    private final AbstractNode                                               rawContext;

    protected PackageControllerTableModelSelectionOnlySelectionInfo(final AbstractNode contextObject, final PackageControllerTableModel<PackageType, ChildrenType> tableModel) {
        super();
        this.rawContext = contextObject;
        this.tableModel = tableModel;
        this.tableModelData = tableModel.getTableData();
        aggregate();
    }

    @Override
    public AbstractNode getRawContext() {
        return rawContext;
    }

    @Override
    protected void aggregate(Queue queue) {
        super.aggregate(null);
    }

    @Override
    protected void aggregate() {
        final ListSelectionModel selectionModel = tableModel.getTable().getSelectionModel();
        if (selectionModel == null || tableModel.isTableSelectionClearing() || selectionModel.isSelectionEmpty()) {
            return;
        }
        final int iMin = selectionModel.getMinSelectionIndex();
        final int iMax = selectionModel.getMaxSelectionIndex();
        if (iMin == -1 || iMax == -1) {
            return;
        }
        if (iMin >= tableModelData.size() || iMax >= tableModelData.size()) {
            throw new IllegalStateException("SelectionModel and TableData missmatch! IMin:" + iMin + "|IMax:" + iMax + "|TableSize:" + tableModelData.size());
        }
        if (selectionModel instanceof DefaultListSelectionModel) {
            try {
                this.selectionModel = (ListSelectionModel) (((DefaultListSelectionModel) selectionModel).clone());
            } catch (CloneNotSupportedException e) {
                this.selectionModel = selectionModel;
            }
        } else {
            this.selectionModel = selectionModel;
        }
        if (rawSelection instanceof ArrayList) {
            ((ArrayList) rawSelection).ensureCapacity(Math.max(1, iMax - iMin));
        }
        final ArrayList<ChildrenType> lastPackageSelectedChildren = new ArrayList<ChildrenType>();
        PackageControllerTableModelDataPackage lastPackage = null;
        boolean lastPackageSelected = false;
        final AtomicInteger lastPackageIndex = new AtomicInteger(0);

        for (int selectionIndex = iMin; selectionIndex <= iMax; selectionIndex++) {
            final AbstractNode node = tableModelData.get(selectionIndex);
            if (node instanceof AbstractPackageNode) {
                final PackageType pkg = (PackageType) node;
                if (lastPackage != null) {
                    aggregatePackagePackageView(lastPackage, lastPackageSelected, lastPackageSelectedChildren);
                    lastPackage = null;
                    lastPackageSelected = false;
                    lastPackageSelectedChildren.clear();
                }
            }
            if (selectionModel.isSelectedIndex(selectionIndex)) {
                rawSelection.add(node);
                if (node instanceof AbstractPackageNode) {
                    final PackageType pkg = (PackageType) node;
                    aggregatePackagePackageView(lastPackage, lastPackageSelected, lastPackageSelectedChildren);
                    lastPackage = getPackageData(lastPackageIndex, pkg);
                    lastPackageSelected = true;
                    lastPackageSelectedChildren.clear();
                } else if (node instanceof AbstractPackageChildrenNode) {
                    final ChildrenType child = (ChildrenType) node;
                    if (lastPackage == null) {
                        final PackageType pkg = getPreviousPackage(selectionIndex);
                        lastPackage = getPackageData(lastPackageIndex, pkg);
                        lastPackageSelected = false;
                    }
                    lastPackageSelectedChildren.add(child);
                }
            }
        }
        aggregatePackagePackageView(lastPackage, lastPackageSelected, lastPackageSelectedChildren);
    }

    private final AtomicBoolean     unselectedChildrenInitialized = new AtomicBoolean(false);
    private ArrayList<ChildrenType> unselectedChildren            = new ArrayList<ChildrenType>();

    @Override
    public synchronized List<ChildrenType> getUnselectedChildren() {
        if (unselectedChildrenInitialized.get() == false && selectionModel != null) {
            unselectedChildrenInitialized.set(true);
            PackageControllerTableModelDataPackage lastPackage = null;
            final AtomicInteger lastPackageIndex = new AtomicInteger(0);
            final int maxSize = tableModelData.size();
            for (int selectionIndex = 0; selectionIndex < maxSize; selectionIndex++) {
                final AbstractNode node = tableModelData.get(selectionIndex);
                if (node instanceof AbstractPackageNode) {
                    if (lastPackage != null) {
                        for (final AbstractNode child : lastPackage.getVisibleChildren()) {
                            unselectedChildren.add((ChildrenType) child);
                        }
                    }
                    lastPackage = getPackageData(lastPackageIndex, (PackageType) node);
                } else if (node instanceof AbstractPackageChildrenNode) {
                    if (!selectionModel.isSelectedIndex(selectionIndex)) {
                        unselectedChildren.add((ChildrenType) node);
                        lastPackage = null;
                    }
                }
            }
        }
        return unselectedChildren;
    }

    private PackageType getPreviousPackage(int currentIndex) {
        for (int index = currentIndex; index >= 0; index--) {
            final AbstractNode node = tableModelData.get(index);
            if (node instanceof AbstractPackageNode) {
                return (PackageType) node;
            }
        }
        throw new WTFException("No PreviousPackage?!");
    }

    private PackageControllerTableModelDataPackage getPackageData(AtomicInteger lastPackageIndex, PackageType currentPackage) {
        final int size = tableModelData.getModelDataPackages().size();
        for (int index = lastPackageIndex.get(); index < size; index++) {
            final PackageControllerTableModelDataPackage next = tableModelData.getModelDataPackages().get(index);
            if (next.getPackage() == currentPackage) {
                lastPackageIndex.set(index);
                return next;
            }
        }
        throw new WTFException("MissMatch between Selection and TableData detected");
    }

    private void aggregatePackagePackageView(final PackageControllerTableModelDataPackage pkgData, final boolean packageSelected, final List<ChildrenType> selectedChildren) {
        if (pkgData != null) {
            final PackageType pkg = (PackageType) pkgData.getPackage();
            final int index = children.size();
            final int size;
            final PackageView<PackageType, ChildrenType> packageView;
            if (pkgData.isExpanded() == false) {
                for (final AbstractNode node : pkgData.getVisibleChildren()) {
                    children.add((ChildrenType) node);
                }
                size = pkgData.getVisibleChildren().size();
                packageView = new PackageView<PackageType, ChildrenType>() {

                    @Override
                    public List<ChildrenType> getChildren() {
                        return children.subList(index, index + size);
                    }

                    @Override
                    public PackageType getPackage() {
                        return (PackageType) pkgData.getPackage();
                    }

                    @Override
                    public boolean isPackageSelected() {
                        return packageSelected;
                    }

                    @Override
                    public boolean isExpanded() {
                        return pkgData.isExpanded();
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return children.subList(index, index + size);
                    }
                };
            } else if (selectedChildren.size() == 0) {
                for (final AbstractNode node : pkgData.getVisibleChildren()) {
                    children.add((ChildrenType) node);
                }
                size = pkgData.getVisibleChildren().size();
                packageView = new PackageView<PackageType, ChildrenType>() {

                    @Override
                    public List<ChildrenType> getChildren() {
                        return children.subList(index, index + size);
                    }

                    @Override
                    public PackageType getPackage() {
                        return (PackageType) pkgData.getPackage();
                    }

                    @Override
                    public boolean isPackageSelected() {
                        return packageSelected;
                    }

                    @Override
                    public boolean isExpanded() {
                        return pkgData.isExpanded();
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return new ArrayList<ChildrenType>(0);
                    }
                };
            } else {
                children.addAll(selectedChildren);
                size = selectedChildren.size();
                packageView = new PackageView<PackageType, ChildrenType>() {

                    @Override
                    public List<ChildrenType> getChildren() {
                        return children.subList(index, index + size);
                    }

                    @Override
                    public PackageType getPackage() {
                        return (PackageType) pkgData.getPackage();
                    }

                    @Override
                    public boolean isPackageSelected() {
                        return packageSelected;
                    }

                    @Override
                    public boolean isExpanded() {
                        return pkgData.isExpanded();
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return children.subList(index, index + size);
                    }
                };
            }
            addPackageView(packageView, pkg);
        }
    }
}