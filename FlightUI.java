package shuttleGuidance;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.math3.util.FastMath;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.MechJeb;
import krpc.client.services.MechJeb.SmartASS;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Engine;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.Orbit;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;

public class FlightUI {

	private static double TGo;
	private Vessel vessel;
	private double pitch = 90;
	private double heading = 90;
	private double roll = 0;
	private FlightMode flightMode = FlightMode.preprogramed;
	private boolean runGUI = false;
	private JLabel utLabel;
	private JLabel metLabel;
	private JLabel headingLabel;
	private JLabel pitchLabel;
	private JLabel rollLabel;
	private JLabel vesselStatusLabel;
	private JLabel throttleLabel;
	private JLabel thrustLabel;
	private JLabel accelLabel;
	private JLabel dymPressureLabel;
	private JLabel atmoDenLabel;
	private JLabel apoLabel;
	private JLabel periLabel;
	private JLabel periodLabel;
	private JLabel tLabel;
	private SpaceCenter spaceCenter;
	private Stream<Double> metStream;
	private Stream<Double> utStream;
	private Stream<SmartASS> sas;
	private Flight flight;
	private ReferenceFrame srfFrame;
	private Stream<Float> atmDen;
	private Stream<Float> qStream;
	private Stream<Float> hSpeedStream;
	private Stream<Float> vSpeedStream;
	private Stream<Float> orbSpeedStream;
	private JLabel horizontalSpeedLabel;
	private JLabel verticalSpeedLabel;
	private JLabel orbSpeedLabel;
	private Stream<Float> altStream;
	private Connection connection;

	public FlightUI() {

		try
		{
			connection = Connection.newInstance("GUI");
			spaceCenter = SpaceCenter.newInstance(connection);
			vessel = spaceCenter.getActiveVessel();
			sas = connection.addStream(MechJeb.class, "getSmartASS");
			metStream = connection.addStream(vessel, "getMET");
			utStream = connection.addStream(SpaceCenter.class, "getUT");
			vessel.getOrbit().getBody().getNonRotatingReferenceFrame();
			srfFrame = vessel.getOrbit().getBody().getReferenceFrame();

			flight = vessel.flight(srfFrame);
			qStream = connection.addStream(flight, "getDynamicPressure");
			atmDen = connection.addStream(flight, "getAtmosphereDensity");

			hSpeedStream = connection.addStream(flight, "getHorizontalSpeed");
			vSpeedStream = connection.addStream(flight, "getVerticalSpeed");
			altStream = connection.addStream(flight, "getMeanAltitude");

			orbSpeedStream = connection.addStream(vessel.getOrbit(), "getSpeed");

		} catch (StreamException | RPCException | IOException e)
		{
			e.printStackTrace();
		}
		initGUI();
		try
		{
			updateGUI();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	private void initGUI()
	{
		Font smallFont = new Font("Monospaced", Font.PLAIN, 25);
		Color bgColor = new Color(51, 255, 51);

		JFrame f = new JFrame("Flight Information");
		f.setSize(1920, 1080);
		Panel panel = new Panel(new GridLayout(9, 9));

		utLabel = new JLabel("N/A");
		utLabel.setForeground(bgColor);
		utLabel.setFont(smallFont);

		metLabel = new JLabel("N/A");
		metLabel.setForeground(bgColor);
		metLabel.setFont(smallFont);

		headingLabel = new JLabel("N/A");
		headingLabel.setForeground(bgColor);
		headingLabel.setFont(smallFont);

		pitchLabel = new JLabel("N/A");
		pitchLabel.setForeground(bgColor);
		pitchLabel.setFont(smallFont);

		rollLabel = new JLabel("N/A");
		rollLabel.setForeground(bgColor);
		rollLabel.setFont(smallFont);

		vesselStatusLabel = new JLabel("N/A");
		vesselStatusLabel.setForeground(bgColor);
		vesselStatusLabel.setFont(smallFont);

		throttleLabel = new JLabel("N/A");
		throttleLabel.setForeground(bgColor);
		throttleLabel.setFont(smallFont);

		thrustLabel = new JLabel("N/A");
		thrustLabel.setForeground(bgColor);
		thrustLabel.setFont(smallFont);

		accelLabel = new JLabel("N/A");
		accelLabel.setForeground(bgColor);
		accelLabel.setFont(smallFont);

		dymPressureLabel = new JLabel("N/A");
		dymPressureLabel.setForeground(bgColor);
		dymPressureLabel.setFont(smallFont);

		atmoDenLabel = new JLabel("N/A");
		atmoDenLabel.setForeground(bgColor);
		atmoDenLabel.setFont(smallFont);

		apoLabel = new JLabel("N/A");
		apoLabel.setForeground(bgColor);
		apoLabel.setFont(smallFont);

		periLabel = new JLabel("N/A");
		periLabel.setForeground(bgColor);
		periLabel.setFont(smallFont);

		periodLabel = new JLabel("N/A");
		periodLabel.setForeground(bgColor);
		periodLabel.setFont(smallFont);

		horizontalSpeedLabel = new JLabel("N/A");
		horizontalSpeedLabel.setForeground(bgColor);
		horizontalSpeedLabel.setFont(smallFont);

		verticalSpeedLabel = new JLabel("N/A");
		verticalSpeedLabel.setForeground(bgColor);
		verticalSpeedLabel.setFont(smallFont);

		orbSpeedLabel = new JLabel("N/A");
		orbSpeedLabel.setForeground(bgColor);
		orbSpeedLabel.setFont(smallFont);

		tLabel = new JLabel("N/A");
		tLabel.setForeground(bgColor);
		tLabel.setFont(smallFont);

		tLabel.setBackground(Color.red);
		tLabel.setOpaque(false);
		f.add(utLabel);

		panel.add(metLabel);
		panel.add(headingLabel);
		panel.add(pitchLabel);
		panel.add(rollLabel);
		panel.add(vesselStatusLabel);
		panel.add(throttleLabel);
		panel.add(thrustLabel);
		panel.add(accelLabel);

		panel.add(dymPressureLabel);
		panel.add(atmoDenLabel);

		panel.add(apoLabel);
		panel.add(periLabel);
		panel.add(periodLabel);

		panel.add(horizontalSpeedLabel);
		panel.add(verticalSpeedLabel);
		panel.add(orbSpeedLabel);

		panel.add(tLabel);
		panel.setBackground(Color.BLACK);
		f.add(panel);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);

	}

	public void updateGUI() throws InterruptedException
	{
		if (runGUI)
		{
			switch (flightMode) {

			case preprogramed:
			case maxQ: {
				updateMissionTime();
				updateFlightAngles();
				setFlightModeLabel();
				updateVehiclePerformace();
				updateVehicleAndSurroundings();
				updateSpeed();

				break;
			}

			default:
				updateMissionTime();
				updateFlightAngles();
				setFlightModeLabel();
				updateVehiclePerformace();
				updateVehicleAndSurroundings();
				updateSpeed();
				updateT();

				break;
			}

		}
	}

	private void updateVehicleAndSurroundings()
	{
		try
		{
			Orbit orbit = vessel.getOrbit();
			double apo = orbit.getApoapsisAltitude();
			double per = orbit.getPeriapsisAltitude();
			double period = orbit.getPeriod();

			double q = qStream.get();
			double atmoDen = atmDen.get();

			dymPressureLabel
					.setText("Vehicle is experiencing " + String.format("%.2f", q / 1.0e3) + " kP of dynamic pressure");
			atmoDenLabel.setText("Surrounding atmo density is " + String.format("%.2f", atmoDen) + " kg/m^3");
			apoLabel.setText("Orbit dimensions " + String.format("%.2f", apo / 1.0e3) + " km x "
					+ String.format("%.2f", per / 1.0e3) + " km");
			periodLabel.setText("Orbit has a " + String.format("%.2f", period / 60.) + " minute period");
			periLabel.setText("Current altitude " + String.format("%.2f", altStream.get()) + " m");

		} catch (RPCException | StreamException e)
		{
			e.printStackTrace();
		}

	}

	private void updateVehiclePerformace()
	{

		try
		{
			double throttle = vessel.getControl().getThrottle();
			double currentEnginesCumlativeThrust = 0;
			for (Engine engine : vessel.getParts().getEngines())
			{
				double thrust = engine.getMaxThrust();
				if (thrust > 1.0e4)
				{
					currentEnginesCumlativeThrust += engine.getThrust();
				}
			}

			double accel = acceleration(vessel, currentEnginesCumlativeThrust);

			throttleLabel.setText(
					"Vehicle is operating at " + String.format("%.2f", throttle * 100.) + " percent max throttle");
			thrustLabel.setText(
					"Vehicle is producing " + String.format("%.2f", currentEnginesCumlativeThrust / 1000.) + " kN");
			accelLabel.setText("Vehicle's acceleration is " + String.format("%.2f", accel) + " m/s^2");

		} catch (RPCException e)
		{
			e.printStackTrace();
		}

	}

	private void updateT()
	{
		if (flightMode != FlightMode.terminalMains)
		{
			tLabel.setText("T to go " + String.format("%.2f", TGo) + " s");
		} else
		{
			tLabel.setText("Pushing apogee to target");

		}
	}

	private void updateSpeed()
	{
		try
		{
			horizontalSpeedLabel.setText("Vehicle is travelling at " + String.format("%.2f", hSpeedStream.get())
					+ " m/s in the surface horizontal");
			verticalSpeedLabel.setText("Vehicle is travelling at " + String.format("%.2f", vSpeedStream.get())
					+ " m/s in the surface vertical");
			orbSpeedLabel.setText("Vehicle's orbital speed is " + String.format("%.2f", orbSpeedStream.get()) + " m/s");
		} catch (RPCException | StreamException e)
		{
			e.printStackTrace();
		}
	}

	private double acceleration(Vessel vessel, double thrust) throws RPCException
	{
		return thrust / vessel.getMass();
	}

	public void setFlightMode(final FlightMode flightMode)
	{
		this.flightMode = flightMode;
	}

	private void setFlightModeLabel()
	{
		vesselStatusLabel.setText("Vehicle is in " + flightMode + " mode");
	}

	private void updateMissionTime()
	{
		try
		{
			utLabel.setText("UT: " + FastMath.round(utStream.get()) + " seconds");
			metLabel.setText("MET: " + FastMath.round(metStream.get()) + " seconds");
		} catch (RPCException | StreamException e)
		{
			e.printStackTrace();
		}
	}

	public void setTGo(double T)
	{
		TGo = T;
	}

	private void updateFlightAngles()
	{
		try
		{
			SmartASS smartASS = sas.get();
			pitch = smartASS.getSurfacePitch();
			heading = smartASS.getSurfaceHeading();
			roll = smartASS.getSurfaceRoll();

			headingLabel.setText("Heading: " + String.format("%.2f", heading) + " degrees from north");
			pitchLabel.setText("Pitch: " + String.format("%.2f", pitch) + " degrees from surface horizon");
			rollLabel.setText("Roll: " + String.format("%.2f", roll) + " degrees from cargobay up");

		} catch (RPCException | StreamException e)
		{
		}
	}

	enum FlightMode {
		preprogramed, maxQ, upfg, terminalMains, evasion, coast, circularization
	}

	public void killLoop()
	{
		runGUI = false;
		try
		{
			connection.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void start()
	{
		runGUI = true;
	}
}
