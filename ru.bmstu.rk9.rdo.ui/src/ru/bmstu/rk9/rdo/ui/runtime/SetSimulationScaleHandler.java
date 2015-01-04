package ru.bmstu.rk9.rdo.ui.runtime;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import ru.bmstu.rk9.rdo.ui.contributions.RDOStatusView;

public class SetSimulationScaleHandler extends AbstractHandler
{
	private static volatile double simulationScale = 5400;

	public static double getSimulationScale()
	{
		return simulationScale;
	}

	public static void setSimulationScale(double scale)
	{
		simulationScale = scale;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		switch(event.getParameter("ru.bmstu.rk9.rdo.ui.runtime.setSimulationScaleParameter"))
		{
			case "+":
				simulationScale *= 1.5;
				break;

			case "-":
				simulationScale /= 1.5d;
				break;

			case "*":
				simulationScale *= 4d;
				break;

			case "/":
				simulationScale /= 4d;
				break;
		}

		SimulationSynchronizer.setSimulationScale(simulationScale);

		RDOStatusView.setSimulationScale(simulationScale);

		return null;
	}
}
