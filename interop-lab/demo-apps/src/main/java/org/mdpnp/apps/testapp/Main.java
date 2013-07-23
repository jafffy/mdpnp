package org.mdpnp.apps.testapp;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws Exception {
	    // TODO this should be external
	    System.setProperty("java.net.preferIPv4Stack","true");

	    Configuration runConf = null;
	    
	    File jumpStartSettings = new File(".JumpStartSettings");
	    
	    boolean cmdline = false;
	    
	    if(args.length > 0) {
	        runConf = Configuration.read(args);
	        cmdline = true;
	    } else if(jumpStartSettings.exists() && jumpStartSettings.canRead()) {
	        FileInputStream fis = new FileInputStream(jumpStartSettings);
	        runConf = Configuration.read(fis);
	        fis.close();
	    }

	    Configuration writeConf = null;
	    
		if(!cmdline) {
		    ConfigurationDialog d = new ConfigurationDialog(runConf);
		    try {
    		    Class<?> cls = Class.forName("com.apple.eawt.Application");
    		    Method m1 = cls.getMethod("getApplication");
    		    Method m2 = cls.getMethod("setDockIconImage", Image.class);
    		    m2.invoke(m1.invoke(null), ImageIO.read(Main.class.getResource("icon.png")));
		    } catch (Throwable t) {
		        log.debug("Not able to set Mac OS X dock icon");
		    }
		    
		    d.setIconImage(ImageIO.read(Main.class.getResource("icon.png")));
		    runConf = d.showDialog();
		    // It's nice to be able to change settings even without running
		    if(null == runConf) {
		        writeConf = d.getLastConfiguration();
		    }
		} else {
		    // fall through to allow configuration via a file
		}
		
		if(null != runConf) {
		    writeConf = runConf;
		}
		
		if(null != writeConf) {
            if(!jumpStartSettings.exists()) {
                jumpStartSettings.createNewFile();
            }
            
            
            if(jumpStartSettings.canWrite()) {
                FileOutputStream fos = new FileOutputStream(jumpStartSettings);
                writeConf.write(fos);
                fos.close();
            }
		}
		
		if(null != runConf) {
		    if(!(Boolean)Class.forName("org.mdpnp.rti.dds.DDS").getMethod("init").invoke(null)) {
                throw new Exception("Unable to DDS.init");
            }

			switch(runConf.getApplication()) {
			case ICE_Device_Interface:
			    new DeviceAdapter().start(runConf.getDeviceType(), runConf.getDomainId(), runConf.getAddress(), !cmdline);
				break;
			case ICE_Supervisor:
			    DemoApp.start(runConf.getDomainId());
			    break;
			}
		} else if(cmdline) {
		    Configuration.help(Main.class, System.out);
		}
		
	}
}
