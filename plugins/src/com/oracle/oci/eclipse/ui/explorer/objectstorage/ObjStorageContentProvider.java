/**
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package com.oracle.oci.eclipse.ui.explorer.objectstorage;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.oci.eclipse.ErrorHandler;
import com.oracle.oci.eclipse.sdkclients.ObjStorageClient;
import com.oracle.oci.eclipse.ui.explorer.RootElement;


public class ObjStorageContentProvider implements ITreeContentProvider, IResourceChangeListener
{
    private static ObjStorageContentProvider instance;
    private TreeViewer treeViewer;
    List<BucketSummary> bucketsList = new ArrayList<BucketSummary>();
    Object storageRootElement;
    boolean foundBuckets = true;

    public ObjStorageContentProvider() {
        instance = this;
    }

    public void getBucketsAndRefresh() {
        bucketsList.clear();
        new Job("Get buckets") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    ObjStorageClient oci = ObjStorageClient.getInstance();
                    bucketsList = oci.getBuckets();
                    if (bucketsList.size() > 0) {
                        foundBuckets = true;
                    } else {
                        foundBuckets = false;
                    }
                } catch (Exception e) {
                    return ErrorHandler.reportException("Unable to get buckets: " + e.getMessage(), e);
                }
                refresh();
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public static ObjStorageContentProvider getInstance() {
        return instance;
    }

    @Override
    public Object[] getChildren(Object parentElement)
    {

        if (parentElement instanceof RootElement)
        {
            return new Object[] {new StorageRootElement()};
        } else if (parentElement instanceof StorageRootElement)
        {
            storageRootElement = parentElement;

            if (bucketsList.size() == 0 && foundBuckets) {
                getBucketsAndRefresh();

                BucketSummary loadingBs = new BucketSummary(null, "Loading...", null, null, null, null, null, null);
                bucketsList.add(loadingBs);
            }
            return bucketsList.toArray();
        } else
        {
            return new Object[0];
        }
    }

    @Override
    public Object getParent(Object element)
    {
        return null;
    }

    @Override
    public boolean hasChildren(Object element)
    {
        return (element instanceof RootElement || element instanceof StorageRootElement);
    }

    @Override
    public Object[] getElements(Object inputElement)
    {
        return getChildren(inputElement);
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
        this.treeViewer = (TreeViewer) viewer;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
    }

    public synchronized void refresh() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                treeViewer.getTree().deselectAll();
                treeViewer.refresh(storageRootElement);
                treeViewer.expandToLevel(1);
            }
        });
    }
}