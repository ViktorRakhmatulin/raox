package ru.bmstu.rk9.rao.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.osgi.service.prefs.BackingStoreException;

import ru.bmstu.rk9.rao.ui.execution.BuildUtil;
import ru.bmstu.rk9.rao.ui.execution.BuildUtil.BuildUtilException;

public class ProjectConfigurator {

	enum ProjectWizardStatus {
		SUCCESS, UNDEFINED_ERROR
	}

	public ProjectConfigurator(final ProjectInfo info) {
		this.info = info;
		root = ResourcesPlugin.getWorkspace().getRoot();
		raoProject = root.getProject(info.getProjectName());
	}

	private final IWorkspaceRoot root;
	private final ProjectInfo info;
	private final IProject raoProject;

	public final ProjectWizardStatus initializeProject() {
		final IServiceLocator serviceLocator = PlatformUI.getWorkbench();
		final IProgressMonitor iProgressMonitor = serviceLocator.getService(IProgressMonitor.class);

		try {
			raoProject.create(iProgressMonitor);
			raoProject.open(iProgressMonitor);
			configureProject();
			createModelFile();
			return ProjectWizardStatus.SUCCESS;
		} catch (CoreException | BackingStoreException | IOException | BuildUtilException e) {
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error",
					"Failed to create project:\n" + e.getMessage());
			e.printStackTrace();
			return ProjectWizardStatus.UNDEFINED_ERROR;
		}
	}

	private final void configureProject() throws BackingStoreException, CoreException, BuildUtilException {
		final ProjectScope projectScope = new ProjectScope(raoProject);
		final IEclipsePreferences projectNode = projectScope.getNode("org.eclipse.core.resources");
		projectNode.node("encoding").put("<project>", "UTF-8");
		projectNode.flush();

		final IProjectDescription description = raoProject.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID, XtextProjectHelper.NATURE_ID });
		raoProject.setDescription(description, null);

		final IFolder sourceFolder = raoProject.getFolder("src-gen");
		sourceFolder.create(false, true, null);

		final List<IClasspathEntry> classpaths = new ArrayList<IClasspathEntry>();

		classpaths.add(JavaCore.newSourceEntry(sourceFolder.getFullPath()));
		classpaths.add(BuildUtil.getJavaSeClasspathEntry());
		classpaths.add(BuildUtil.getRaoxClasspathEntry());
		classpaths.add(BuildUtil.getXtendClasspathEntry());

		final IJavaProject game5JavaProject = JavaCore.create(raoProject);
		game5JavaProject.setRawClasspath(classpaths.toArray(new IClasspathEntry[classpaths.size()]), null);
	}

	private final void createModelFile() throws CoreException, IOException {
		final String modelName = info.getProjectName() + ".rao";
		final IPath modelIPath = root.getLocation().append(raoProject.getFullPath()).append(modelName);
		final File modelFile = new File(modelIPath.toString());
		final IFile modelIFile = raoProject.getFile(modelName);

		modelFile.createNewFile();
		fillModel(modelIFile);
		raoProject.refreshLocal(IResource.DEPTH_INFINITE, null);
		IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), modelIFile);
	}

	private final void createGraphicProcessEditor() throws CoreException, IOException {
		final String processName = info.getProjectName() + ".proc";
		final IPath modelIPath = root.getLocation().append(raoProject.getFullPath()).append(processName);
		final File processFile = new File(modelIPath.toString());
		final IFile processIFile = raoProject.getFile(processName);

		processFile.createNewFile();
		raoProject.refreshLocal(IResource.DEPTH_INFINITE, null);
		IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), processIFile);
	}

	private final void fillModel(final IFile modelIFile) throws CoreException, IOException {
		final String modelTemplatePath;
		switch (info.getTemplate()) {
		case NO_TEMPLATE:
			return;
		case BARBER_SIMPLE:
			modelTemplatePath = "/model_templates/barber_simple.rao.template";
			break;
		case BARBER_EVENTS:
			modelTemplatePath = "/model_templates/barber_events.rao.template";
			break;
		case BARBER_CLIENTS:
			modelTemplatePath = "/model_templates/barber_clients.rao.template";
			break;
		case GRAPHIC_PROCESS:
			createGraphicProcessEditor();
			return;
		default:
			return;
		}

		InputStream inputStream = ProjectConfigurator.class.getClassLoader().getResourceAsStream(modelTemplatePath);
		modelIFile.create(inputStream, true, null);
	}
}
