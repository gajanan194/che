/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.multiuser.api.distributed.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.workspace.server.WorkspaceStatusCache;
import org.jgroups.JChannel;
import org.jgroups.blocks.ReplicatedHashMap;

/** @author Anton Korneta */
@Singleton
public class JGroupsWorkspaceStatusCache implements WorkspaceStatusCache {

  private static final String CHANNEL_NAME = "WorkspaceStateCache";

  private final ReplicatedHashMap<String, WorkspaceStatus> delegate;
  private final Map<String, WorkspaceStatus> localStatuses = new ConcurrentHashMap<>();

  @Inject
  public JGroupsWorkspaceStatusCache(@Named("jgroups.config.file") String confFile) {
    try {
      JChannel channel = new JChannel(confFile);
      channel.connect(CHANNEL_NAME);
      delegate = new ReplicatedHashMap<>(channel);
      delegate.setBlockingUpdates(true);
      delegate.start(5000);
    } catch (Exception ex) {
      throw new RuntimeException("Jgroups cache creation failed. Cause :" + ex.getMessage());
    }
  }

  @Override
  public WorkspaceStatus get(String workspaceId) {
    return delegate.get(workspaceId);
  }

  @Override
  public WorkspaceStatus replace(String workspaceId, WorkspaceStatus newStatus) {
    localStatuses.replace(workspaceId, newStatus);
    return delegate.replace(workspaceId, newStatus);
  }

  @Override
  public boolean replace(
      String workspaceId, WorkspaceStatus prevStatus, WorkspaceStatus newStatus) {
    localStatuses.replace(workspaceId, prevStatus, newStatus);
    return delegate.replace(workspaceId, prevStatus, newStatus);
  }

  @Override
  public WorkspaceStatus remove(String workspaceId) {
    localStatuses.remove(workspaceId);
    return delegate.remove(workspaceId);
  }

  @Override
  public WorkspaceStatus putIfAbsent(String workspaceId, WorkspaceStatus status) {
    localStatuses.putIfAbsent(workspaceId, status);
    return delegate.putIfAbsent(workspaceId, status);
  }

  @Override
  public Map<String, WorkspaceStatus> localStates() {
    return new HashMap<>(localStatuses);
  }
}
