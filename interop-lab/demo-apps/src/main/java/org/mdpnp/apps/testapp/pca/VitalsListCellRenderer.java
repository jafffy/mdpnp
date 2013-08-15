package org.mdpnp.apps.testapp.pca;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.LineBorder;

import org.mdpnp.apps.testapp.Device;
import org.mdpnp.apps.testapp.DeviceIcon;
import org.mdpnp.apps.testapp.DeviceListModel;
import org.mdpnp.apps.testapp.vital.Value;
import org.mdpnp.apps.testapp.vital.Vital;

public class VitalsListCellRenderer extends JPanel implements ListCellRenderer {

		private final JLabel name = new JLabel(" ");
		private final JLabel deviceName = new JLabel(" ");
		private final JLabel value = new JLabel(" ");
		private final JLabel icon = new JLabel(" ");
		private final JLabel udi = new JLabel(" ");
		
		private final DeviceListModel deviceListModel;
		
		public VitalsListCellRenderer(DeviceListModel deviceListModel) {
			super(new BorderLayout());
			this.deviceListModel = deviceListModel;
			setBorder(new LineBorder(Color.gray, 1));
			setOpaque(false);
			name.setFont(name.getFont().deriveFont(24f));
			value.setFont(value.getFont().deriveFont(24f));
			deviceName.setFont(deviceName.getFont().deriveFont(14f));
			udi.setFont(Font.decode("fixed-10"));
//			value.setFont(value.getFont().deriveFont(14f));
			JPanel pan = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints(0,0,1,1,1.0,1.0,GridBagConstraints.CENTER,GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
			pan.setOpaque(false);
			
			gbc.weightx = 0.9;
			pan.add(name, gbc);
			gbc.gridy++;
			pan.add(deviceName, gbc);
			
			gbc.gridx = 1;
			gbc.weightx = 1.1;
			gbc.gridy = 0;
			pan.add(value, gbc);
			gbc.gridy++;
			pan.add(udi, gbc);
			
			add(icon, BorderLayout.WEST);
			add(pan, BorderLayout.CENTER);
			
		}
		
		@Override
		public Component getListCellRendererComponent(JList list, Object val,
				int index, boolean isSelected, boolean cellHasFocus) {
			Vital vital = (Vital) val;
			// strongly thinking about making these identifiers into strings
			String name = vital.getLabel();
			String units = vital.getUnits();

			this.name.setText(name);
			String s = "";
			if(vital.getValues().isEmpty()) {
			    s = "<NO SOURCES>";
			    icon.setIcon(null);
			    deviceName.setText("");
			    udi.setText("");
			} else {
    			Device device = deviceListModel.getByUniversalDeviceIdentifier(vital.getValues().get(0).getUniversalDeviceIdentifier());
    			if(null != device) {
    			    DeviceIcon di = device.getIcon();
                  if(null != di) {
                      icon.setIcon(new ImageIcon(di.getImage()));
                  }
                    deviceName.setText(device.getMakeAndModel());
                    udi.setText(device.getShortUDI());
    			} else {
    			    icon.setIcon(null);
    			    deviceName.setText("");
    			    udi.setText(vital.getValues().get(0).getUniversalDeviceIdentifier().substring(0, Device.SHORT_UDI_LENGTH));
    			}
    			for(Value v : vital.getValues()) {
    			    s += v.getNumeric().value + " ";
    			}
    			s+=units;
			}
			value.setText(s);
			
//			DeviceIcon di = v.getDevice().getIcon();
//			if(null != di) {
//			    icon.setIcon(new ImageIcon(di.getImage()));
//			}
//			deviceName.setText(v.getDevice().getMakeAndModel());
//			udi.setText(v.getDevice().getShortUDI());
			return this;
		}
		
	}