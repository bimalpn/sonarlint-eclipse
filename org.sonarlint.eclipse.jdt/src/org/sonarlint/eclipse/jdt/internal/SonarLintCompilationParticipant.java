/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.jdt.internal;

import java.util.Arrays;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.jobs.AbstractAnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class SonarLintCompilationParticipant extends CompilationParticipant {

  @Override
  public boolean isActive(IJavaProject project) {
    ISonarLintProject sonarLintProject = Adapters.adapt(project.getProject(), ISonarLintProject.class);
    return sonarLintProject != null && sonarLintProject.isOpen();
  }

  @Override
  public void reconcile(ReconcileContext context) {
    try {
      ICompilationUnit cu = context.getWorkingCopy();
      if (!cu.isStructureKnown()) {
        return;
      }
      SonarLintLogger.get().info("AST level: " + context.getASTLevel());
      context.getAST(context.getASTLevel());
      IResource resource = cu.getUnderlyingResource();
      ISonarLintFile sonarLintFile = Adapters.adapt(resource, ISonarLintFile.class);
      AnalyzeProjectRequest request = new AnalyzeProjectRequest(Adapters.adapt(resource.getProject(), ISonarLintProject.class),
        Arrays.asList(new AnalyzeProjectRequest.FileWithDocument(sonarLintFile, sonarLintFile.getDocument())),
        TriggerType.COMPILATION);
      AbstractAnalyzeProjectJob.create(request).schedule();
    } catch (CoreException e) {
      SonarLintLogger.get().error("Error while analyzing resource", e);
    }
  }

}
