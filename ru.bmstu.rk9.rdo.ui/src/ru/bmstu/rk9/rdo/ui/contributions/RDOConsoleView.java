package ru.bmstu.rk9.rdo.ui.contributions;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

public class RDOConsoleView extends ViewPart
{
	public static final String ID = "ru.bmstu.rk9.rdo.ui.RDOConsoleView"; //$NON-NLS-1$

	private static StyledText styledText;

	private static String text = "";

	@Override
	public void createPartControl(Composite parent)
	{
		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.NONE);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		{
			styledText = new StyledText(scrolledComposite, SWT.H_SCROLL | SWT.V_SCROLL);
			styledText.setAlwaysShowScrollBars(false);
			redrawText();
			styledText.setEditable(false);
			styledText.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));
			styledText.setLeftMargin(2);
			styledText.setTopMargin (5);

			Menu popupMenu = new Menu(styledText);
			MenuItem copy = new MenuItem(popupMenu, SWT.CASCADE);
			copy.setText("Copy\tCtrl+C");
			copy.setAccelerator(SWT.CTRL + 'C');
			copy.addSelectionListener
			(
				new SelectionAdapter()
				{
					public void widgetSelected(SelectionEvent event)
					{
						styledText.invokeAction(ST.COPY);
					}
				}
			);
			styledText.setMenu(popupMenu);
		}
		scrolledComposite.setContent(styledText);
		scrolledComposite.setMinSize(styledText.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    
		registerTextFontUpdateListener();
		updateTextFont();
	}

	private void updateTextFont()
	{
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		ITheme currentTheme = themeManager.getCurrentTheme();
		FontRegistry fontRegistry = currentTheme.getFontRegistry();
		styledText.setFont(fontRegistry.get(PreferenceConstants.EDITOR_TEXT_FONT));
	}

	public static void clearConsoleText()
	{
		text = "";
		redrawText();
	}

	public static void addLine(String line)
	{
		text = text + line + "\n";
		redrawText();
	}

	public static void appendText(String add)
	{
		text = text + add;
		redrawText();
	}

	public static void redrawText()
	{
		if (styledText != null)
			styledText.getDisplay().asyncExec
			(
				new Runnable ()
				{
					public void run ()
					{
						styledText.setText(text);
					}
				}
			);
	}

	private void registerTextFontUpdateListener()
	{
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		IPropertyChangeListener listener = new IPropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent event)
			{
				if (event.getProperty().equals(PreferenceConstants.EDITOR_TEXT_FONT))
					updateTextFont();
			}
		};
		themeManager.addPropertyChangeListener(listener);
	}

	@Override
	public void setFocus() {}

}
