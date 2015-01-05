package ru.bmstu.rk9.rdo.ui.contributions;

import java.util.TimerTask;
import java.util.ArrayList;
import java.util.EnumMap;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ru.bmstu.rk9.rdo.lib.Simulator;
import ru.bmstu.rk9.rdo.lib.Subscriber;
import ru.bmstu.rk9.rdo.lib.Tracer.TraceType;
import ru.bmstu.rk9.rdo.lib.Tracer.TraceOutput;

public class RDOTraceView extends ViewPart
{
	public static final String ID = "ru.bmstu.rk9.rdo.ui.RDOTraceView";

	static TableViewer viewer;

	@Override
	public void createPartControl(Composite parent)
	{
		createViewer(parent);
	}

	private final void createViewer(Composite parent)
	{
		viewer = new TableViewer(
			parent,
			SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL
		);

		FontRegistry fontRegistry =
			PlatformUI
			.getWorkbench()
			.getThemeManager()
			.getCurrentTheme()
			.getFontRegistry();

		Menu popupMenu = new Menu(viewer.getTable());
		MenuItem copy = new MenuItem(popupMenu, SWT.CASCADE);
		copy.setText("Copy\tCtrl+C");
		copy.addSelectionListener(
			new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent event)
				{
					copyTraceLine();
				}
			}
		);
		viewer.getTable().setMenu(popupMenu);

		viewer.getTable().addKeyListener(
			new KeyListener()
			{
				@Override
				public void keyReleased(KeyEvent e)
				{
					if (e.keyCode == 'c')
						stillHolding = false;
				}

				@Override
				public void keyPressed(KeyEvent e)
				{
					if (((e.stateMask & SWT.CTRL) == SWT.CTRL) &&
							(e.keyCode == 'c'))
					{
						if (stillHolding)
							return;

						stillHolding = true;
						copyTraceLine();
					}
				}

				private boolean stillHolding = false;
			}
		);

		viewer.setContentProvider(new RDOTraceViewContentProvider());
		viewer.setLabelProvider(new RDOTraceViewLabelProvider());
		viewer.setUseHashlookup(true);
		viewer.getTable().setFont(
			fontRegistry.get(PreferenceConstants.EDITOR_TEXT_FONT));

		viewer.addSelectionChangedListener(
			new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event)
				{
					if (viewer.getTable().getSelectionIndex() !=
							viewer.getTable().getItemCount() - 1)
						shouldFollowOutput = false;
					else
						shouldFollowOutput = true;
				}
			});

		if(Simulator.isInitialized())
		{
			ArrayList<TraceOutput> traceList = Simulator.getTracer().getTraceList();
			RDOTraceView.viewer.setInput(traceList);
			RDOTraceView.viewer.setItemCount(traceList.size());
			viewer.refresh();
		}
	}

	private static void copyTraceLine()
	{
		String text = viewer.getTable().getSelection()[0].getText(0);
		TextTransfer textTransfer = TextTransfer.getInstance();
		Clipboard clipboard = new Clipboard(
			PlatformUI.getWorkbench().getDisplay());
		clipboard.setContents(
			new Object[] {text},
			new Transfer[] {textTransfer}
		);
		clipboard.dispose();
	}

	private static boolean shouldFollowOutput = true;

	private static boolean haveNewRealTimeData = false;

	private static boolean shouldFollowOutput()
	{
		return shouldFollowOutput;
	}

	public static final Subscriber realTimeUpdater =
		new Subscriber()
		{
			@Override
			public void fireChange()
			{
				haveNewRealTimeData = true;
			}
		};

	public static TimerTask getRealTimeUpdaterTask()
	{
		return new TimerTask()
		{
			private final Display display = PlatformUI.getWorkbench().getDisplay();
			private final Runnable updater = new Runnable()
			{
				@Override
				public void run()
				{
					final ArrayList<TraceOutput> traceList =
						Simulator.getTracer().getTraceList();
					final int size = traceList.size();

					RDOTraceView.viewer.setItemCount(size);
					if (RDOTraceView.shouldFollowOutput())
						RDOTraceView.viewer.getTable().setTopIndex(size - 1);
				}
			};

			@Override
			public void run()
			{
				if (haveNewRealTimeData && readyForInput() && !display.isDisposed())
				{
					haveNewRealTimeData = false;
					display.asyncExec(updater);
				}
			}
		};
	}

	public static final Subscriber commonUpdater =
		new Subscriber()
		{
			@Override
			public void fireChange()
			{
				final ArrayList<TraceOutput> traceList =
					Simulator.getTracer().getTraceList();
				final int size = traceList.size();
				PlatformUI.getWorkbench().getDisplay().asyncExec(
					new Runnable()
					{
						@Override
						public void run()
						{
							RDOTraceView.viewer.setInput(traceList);
							RDOTraceView.viewer.setItemCount(size);

							if (RDOTraceView.shouldFollowOutput())
								RDOTraceView.viewer.getTable().setTopIndex(size - 1);

							viewer.refresh();
						}
					}
				);
			}
		};

	public final static boolean readyForInput()
	{
		return
			viewer != null
			&& !viewer.getTable().isDisposed()
			&& viewer.getContentProvider() != null
			&& viewer.getLabelProvider() != null;
	}

	@Override
	public void setFocus() {}
}

class RDOTraceViewContentProvider implements ILazyContentProvider
{
	private ArrayList<TraceOutput> traceList;

	@Override
	public void dispose() {}

	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		traceList = (ArrayList<TraceOutput>) newInput;
	}

	@Override
	public void updateElement(int index)
	{
		//TODO completely avoid that situation
		if (traceList != null && index < traceList.size())
			RDOTraceView.viewer.replace(traceList.get(index), index);
	}
}

class RDOTraceViewLabelProvider implements ILabelProvider, IColorProvider
{
	private final EnumMap<TraceType, TraceColor> colorByType =
		new EnumMap<TraceType, TraceColor>(TraceType.class);

	RDOTraceViewLabelProvider()
	{
		initializeColorMap();
	}

	private class TraceColor
	{
		private final Color foregroundColor;
		private final Color backgroundColor;

		public TraceColor(Color fg, Color bg)
		{
			foregroundColor = fg;
			backgroundColor = bg;
		}

		public final Color foregroundColor()
		{
			return foregroundColor;
		}

		public final Color backgroundColor()
		{
			return backgroundColor;
		}
	}

	private final void initializeColorMap()
	{
		Display display = PlatformUI.getWorkbench().getDisplay();
		colorByType.put(
			TraceType.RESOURCE_CREATE,
			new TraceColor(
				new Color(display, 0x23, 0x74, 0x42),
				new Color(display, 0x96, 0xFF, 0x96)
			)
		);

		colorByType.put(
			TraceType.RESOURCE_KEEP,
			new TraceColor(
				new Color(display, 0x00, 0x86, 0x00),
				new Color(display, 0xD0, 0xFF, 0xD0)
			)
		);

		colorByType.put(
			TraceType.RESOURCE_ERASE,
			new TraceColor(
				new Color(display, 0x43, 0x5A, 0x43),
				new Color(display, 0xB4, 0xE0, 0xB4)
			)
		);

		colorByType.put(
			TraceType.SYSTEM,
			new TraceColor(
				new Color(display, 0x8B, 0x00, 0x00),
				new Color(display, 0xFF, 0xC0, 0xCB)
			)
		);

		colorByType.put(
			TraceType.OPERATION_BEGIN,
			new TraceColor(
				new Color(display, 0x34, 0x4B, 0xA2),
				new Color(display, 0xAA, 0xE3, 0xFB)
			)
		);

		colorByType.put(
			TraceType.OPERATION_END,
			new TraceColor(
				new Color(display, 0x16, 0x02, 0x50),
				new Color(display, 0x81, 0xB0, 0xD5)
			)
		);

		colorByType.put(
			TraceType.EVENT,
			new TraceColor(
				new Color(display, 0x4F, 0x29, 0x62),
				new Color(display, 0xD0, 0xD0, 0xFF)
			)
		);

		colorByType.put(
			TraceType.RULE,
			new TraceColor(
				new Color(display, 0x17, 0x32, 0x47),
				new Color(display, 0xB6, 0xCB, 0xDB)
			)
		);

		colorByType.put(
			TraceType.RESULT,
			new TraceColor(
				new Color(display, 0x00, 0x00, 0x00),
				new Color(display, 0xF1, 0xFB, 0xE2)
			)
		);

		colorByType.put(
			TraceType.SEARCH_BEGIN,
			new TraceColor(
				new Color(display, 0x5A, 0x4F, 0x37),
				new Color(display, 0xF8, 0xD6, 0x8D)
			)
		);

		colorByType.put(
			TraceType.SEARCH_OPEN,
			new TraceColor(
				new Color(display, 0x4B, 0x54, 0x0E),
				new Color(display, 0xE6, 0xF1, 0x98)
			)
		);

		colorByType.put(
			TraceType.SEARCH_SPAWN_NEW,
			new TraceColor(
				new Color(display, 0x00, 0x54, 0x72),
				new Color(display, 0xE8, 0xE8, 0xD7)
			)
		);

		colorByType.put(
			TraceType.SEARCH_SPAWN_WORSE,
			new TraceColor(
				new Color(display, 0x69, 0x55, 0x49),
				colorByType.get(TraceType.SEARCH_SPAWN_NEW).backgroundColor()
			)
		);

		colorByType.put(
			TraceType.SEARCH_SPAWN_BETTER,
			new TraceColor(
				new Color(display, 0x8B, 0x00, 0x00),
				colorByType.get(TraceType.SEARCH_SPAWN_NEW).backgroundColor()
			)
		);

		colorByType.put(
			TraceType.SEARCH_RESOURCE_KEEP,
			colorByType.get(TraceType.RESOURCE_KEEP)
		);

		colorByType.put(
			TraceType.SEARCH_DECISION,
			new TraceColor(
				new Color(display, 0x54, 0x1E, 0x09),
				new Color(display, 0xF7, 0xCF, 0xB5)
			)
		);

		colorByType.put(
			TraceType.SEARCH_END_ABORTED,
			new TraceColor(
				new Color(display, 0xF0, 0x4B, 0x30),
				new Color(display, 0xE3, 0xF0, 0xF6)
			)
		);

		colorByType.put(
			TraceType.SEARCH_END_CONDITION,
			new TraceColor(
				new Color(display, 0x54, 0x1E, 0x09),
				new Color(display, 0xF0, 0xDE, 0xDB)
			)
		);

		colorByType.put(
			TraceType.SEARCH_END_SUCCESS,
			colorByType.get(TraceType.SEARCH_END_CONDITION)
		);

		colorByType.put(
			TraceType.SEARCH_END_FAIL,
			new TraceColor(
				new Color(display, 0xF0, 0x4B, 0x30),
				colorByType.get(TraceType.SEARCH_END_SUCCESS).backgroundColor()
			)
		);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {}

	@Override
	public void dispose()
	{
		for (final TraceColor color : colorByType.values())
		{
			color.foregroundColor().dispose();
			color.backgroundColor().dispose();
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property)
	{
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {}

	@Override
	public Image getImage(Object element)
	{
		return null;
	}

	@Override
	public String getText(Object element)
	{
		return ((TraceOutput) element).content();
	}

	@Override
	public Color getForeground(Object element)
	{
		TraceType type = ((TraceOutput) element).type();
		return colorByType.get(type).foregroundColor();
	}

	@Override
	public Color getBackground(Object element)
	{
		TraceType type = ((TraceOutput) element).type();
		return colorByType.get(type).backgroundColor();
	}
}