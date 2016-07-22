package wafflestomper.quiettime;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ConfigManager {
	
	private Configuration config;
	protected boolean chatDisabledAtStartup = false;
	protected boolean snitchHover = true;
	protected boolean snitchRateLimiting = false;
	protected long snitchSecondsBetweenMessages = 10l;
	
	
	public ConfigManager(){}
	
	
	public void preInit(FMLPreInitializationEvent event){
	    this.config = new Configuration(event.getSuggestedConfigurationFile());
	    this.config.load();
	    
	    // Chat disabling (hiding)
	    this.chatDisabledAtStartup = this.config.get("chat_hiding", "chat_disabled_at_startup", false, 
	    		"Disables chat by default. Note that you can still toggle it on and off in game").getBoolean(false);
	    
	    // Snitch hovering conversion
	    this.snitchHover = this.config.get("snitch_censoring", "snitch_hover", true, 
	    		"Hides snitch co-ordinates in chat by converting them to hovertext").getBoolean(true);
	    
	    // Snitch rate limiting
	    this.snitchRateLimiting = this.config.get("snitch_rate_limiting", "snitch_rate_limiting_enable", false, 
	    		"Master enable switch for rate limiting").getBoolean(false);
	    this.snitchSecondsBetweenMessages = this.config.get("snitch_rate_limiting", "seconds_between_messages", 10l, 
	    		"Minimum number of seconds").getLong(10l);
	    
	    
	    this.config.save();
    }
}
