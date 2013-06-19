package org.mdpnp.android.pulseox;

import org.mdpnp.data.IdentifiableUpdate;
import org.mdpnp.data.text.TextUpdate;
import org.mdpnp.data.waveform.WaveformUpdate;
import org.mdpnp.data.numeric.NumericUpdate;
import org.mdpnp.data.enumeration.EnumerationUpdate;
import org.mdpnp.devices.AbstractDevice;
import org.mdpnp.devices.connected.AbstractGetConnected;
import org.mdpnp.devices.masimo.radical.DemoRadical7;
import org.mdpnp.devices.nellcor.pulseox.DemoN595;
import org.mdpnp.devices.nonin.pulseox.DemoPulseOx;
import org.mdpnp.devices.nonin.pulseox.DemoPulseOx.Bool;
import org.mdpnp.devices.serial.SerialProviderFactory;
import org.mdpnp.devices.simulation.pulseox.SimPulseOximeter;
import org.mdpnp.guis.waveform.WaveformUpdateWaveformSource;
import org.mdpnp.messaging.Gateway;
import org.mdpnp.messaging.GatewayListener;
import org.mdpnp.nomenclature.ConnectedDevice;
import org.mdpnp.nomenclature.Device;
import org.mdpnp.nomenclature.PulseOximeter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class PulseOxActivity extends Activity implements GatewayListener {
	private AbstractDevice pulseox;
	private WaveformRepresentation wave;
	private TextView heartRate, spo2, state, name, guid;
	private BluetoothDevice device;
	
	private Gateway gateway; // = new Gateway();
	
	private WaveformUpdateWaveformSource wuws; // = new WaveformUpdateWaveformSource();
	
	private static final String logName = PulseOxActivity.class.getName();
	private static final void debug(String str) {
		Log.d(logName, str);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		debug("*** onStart ***");
		gateway.addListener(this);
		getConnected = new GetConnected(null==device?"":device.getAddress(), gateway);
		if(null == pulseox) {
			if(null == device) {
				pulseox = new SimPulseOximeter(gateway);
			} else if(device.getName().startsWith("Nellcor")) {
				try {
					pulseox = new DemoN595(gateway);
				} catch (Exception e) {
					Log.e(PulseOxActivity.class.getName(), "Unable to construct", e);
				}
			} else if(device.getName().startsWith("Masimo")) {
				try {
					pulseox = new DemoRadical7(gateway);
				} catch (Exception e) {
					Log.e(PulseOxActivity.class.getName(), "Unable to construct", e);
				}
			} else {
				pulseox = new DemoPulseOx(gateway);
			}
			
		}
		wave.setSource(wuws);
	}
	@Override
	protected void onStop() {
		debug("*** onStop ***");
		gateway.removeListener(this);
        if(null != wave) {
        	wave.setSource(null);
        }
        
        if(null != pulseox) {
        	gateway.removeListener(pulseox);
			pulseox = null;
		}
        getConnected = null;
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		debug("*** onPause ***");
		wave.pause();
		// Thing here is that as part of connecting a pairing dialog may
		// be presented and trigger this onPause... so we must allow a continuation of
		// the pairing process across onPause/onResume
		ConnectedDevice.State state = this.lastState;
		if(state != null) {
			switch(state) {
			case Connected:
			case Negotiating:
				getConnected.disconnect();
				break;
			default:
			}
		}
//		getConnected.disconnect();
	}
	
	private static class GetConnected extends AbstractGetConnected {
		private final String address;
		
		GetConnected(String address, Gateway gateway) {
			super(gateway);
			this.address = address;
		}
		
		@Override
		protected void abortConnect() {
			
		}

		@Override
		protected String addressFromUser() {
			return address;
		}

		@Override
		protected String addressFromUserList(String[] list) {
			return address;
		}
		@Override
		protected boolean isFixedAddress() {
			return true;
		}
	}
	private GetConnected getConnected;
	
	@Override
	protected void onResume() {
		debug("*** onResume ***");
		super.onResume();
		wave.resume();
		getConnected.connect();
	}
	

	
	@Override
	protected void onDestroy() {
		debug("*** onDestroy ***");
		super.onDestroy();

		this.device = null;
		this.wuws = null;
		this.gateway = null;
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		debug("*** onRetainNonConfigurationInstance ***");
		return device;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		debug("*** onSaveInstanceState ***");
		super.onSaveInstanceState(outState);
		outState.putParcelable("DEVICE", device);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		debug("*** onCreate ***");
        super.onCreate(savedInstanceState);
        gateway = new Gateway();
        wuws = new WaveformUpdateWaveformSource();
        
        SerialProviderFactory.setDefaultProvider(new BluetoothSerialProvider());
        setContentView(R.layout.activity_pulse_ox);

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        wave = (WaveformRepresentation)findViewById(R.id.waveform);
		heartRate = ((TextView)findViewById(R.id.heart_rate));
		spo2 = ((TextView)findViewById(R.id.spo2)); 	
		name = ((TextView) findViewById(R.id.name));
		guid = ((TextView) findViewById(R.id.guid));
		state = ((TextView) findViewById(R.id.status));

		noValue = getString(R.string.no_value);
        
        Object lnci = getLastNonConfigurationInstance();
        if(null == lnci) {
        	device = getIntent().getParcelableExtra("DEVICE");
        	if(device == null && savedInstanceState != null) {
        		device = savedInstanceState.getParcelable("DEVICE");
        	}
        } else {
        	this.device = (BluetoothDevice) lnci;

        }
    }

	private static class DeviceUpdate implements Runnable {
		private TextView textView;
		private String value;
		 
		public DeviceUpdate(TextView textView, String value) {
			set(textView, value);
		}
		
		public DeviceUpdate set(TextView textView, String value) {
			this.textView = textView;
			this.value = value;
			return this;
		}
		public void run() {
			textView.setText(value);
		}
	}

	private String noValue;
	
	private ConnectedDevice.State lastState;
	private String lastConnectionInfo;
	
	private void doStateAndConnectionInfo() {
		runOnUiThread(new DeviceUpdate(this.state, (null == lastState ? noValue : lastState.toString())+((null == lastConnectionInfo || "".equals(lastConnectionInfo)) ? "" : ("("+lastConnectionInfo+")")) ));
	}

	private boolean outOfTrack = false;
	
	@Override
	public void update(IdentifiableUpdate<?> update) {
		if(Device.NAME.equals(update.getIdentifier())) {
			runOnUiThread(new DeviceUpdate(name, ((TextUpdate)update).getValue()));
		} else if(Device.GUID.equals(update.getIdentifier())) {
			runOnUiThread(new DeviceUpdate(guid, ((TextUpdate)update).getValue()));
		} else if(PulseOximeter.SPO2.equals(update.getIdentifier())) {
			Number number = ((NumericUpdate)update).getValue();
			if(number!=null&&number.intValue()>100) {
				number = null;
			}
			runOnUiThread(new DeviceUpdate(spo2, null == number ? noValue :Integer.toString(number.intValue()))); 
		} else if(PulseOximeter.PULSE.equals(update.getIdentifier())) {
			Number number = ((NumericUpdate)update).getValue();
			if(number!=null&&number.intValue()>250) {
				number = null;
			}
			runOnUiThread(new DeviceUpdate(heartRate, null == number ? noValue :Integer.toString(number.intValue())));
		} else if(PulseOximeter.PLETH.equals(update.getIdentifier())) {
			wuws.applyUpdate((WaveformUpdate) update);
		} else if(ConnectedDevice.STATE.equals(update.getIdentifier())) {
			ConnectedDevice.State newState = (ConnectedDevice.State) ((EnumerationUpdate)update).getValue(); 
			if((lastState == null || !ConnectedDevice.State.Connected.equals(lastState)) &&
				newState != null && ConnectedDevice.State.Connected.equals(newState)) {
				Log.d(logName, "Resetting the WaveformUpdateWaveformSource where newly Connected");
				wuws.reset();
			}
			lastState = newState;
			doStateAndConnectionInfo();
		} else if(ConnectedDevice.CONNECTION_INFO.equals(update.getIdentifier())) {
			lastConnectionInfo = ((TextUpdate)update).getValue();
			doStateAndConnectionInfo();
		} else if(DemoPulseOx.PERFUSION.equals(update.getIdentifier())) {
		    DemoPulseOx.Perfusion perfusion = (DemoPulseOx.Perfusion) ((EnumerationUpdate)update).getValue(); 
			if(null != perfusion) {
				switch(perfusion) {
				case Green:
					spo2.setTextColor(getResources().getColor(R.color.greenTextColor));
					break;
				case Red:
					spo2.setTextColor(getResources().getColor(R.color.redTextColor));
					break;
				case Yellow:
					spo2.setTextColor(getResources().getColor(R.color.yellowTextColor));
					break;
				default:
					break;
				
				}
			} else {
				spo2.setTextColor(getResources().getColor(R.color.greenTextColor));
			}
		} else if(DemoPulseOx.FIRMWARE_REVISION.equals(update.getIdentifier())) {
			
		} else if(DemoPulseOx.OUT_OF_TRACK.equals(update.getIdentifier())) {
			Bool _outOfTrack = (Bool) ((EnumerationUpdate)update).getValue();
			boolean outOfTrack = Bool.True.equals(_outOfTrack);
			if(this.outOfTrack ^ outOfTrack) {
				this.outOfTrack = outOfTrack;
				if(!outOfTrack) {
					Log.d(logName, "NO LONGER OUT OF TRACK");
					wave.setOutOfTrack(false);
					wave.setForeground(getResources().getColor(R.color.greenTextColor));
					wuws.reset();
				} else {
					wave.setOutOfTrack(true);
					wave.setForeground(getResources().getColor(R.color.yellowTextColor));
					Log.d(logName, "SENSOR OUT OF TRACK");
				}
			}
		}
	}

}