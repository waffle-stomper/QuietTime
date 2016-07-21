package wafflestomper.quiettime;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;

public class QuietTimeGui extends GuiIngameMenu{
	
	private GuiButton btnChatToggle;
	private static final int BTN_CHAT_TOGGLE = 1337;
	private QuietTime qtRef;
	
	
	public QuietTimeGui(QuietTime _qtRef){
		super();
		this.qtRef = _qtRef;
	}
	
	
	@Override
    public void initGui(){
		super.initGui();
    	int buttonWidth = 120;
        int buttonHeight = 20;
        String chatButtonText = "Disable Chat";
        if (qtRef.isChatDisabled()){
        	chatButtonText = "Enable Chat";
        }
    	this.btnChatToggle = new GuiButton(BTN_CHAT_TOGGLE, 0, 0, buttonWidth, buttonHeight, chatButtonText);
    	this.buttonList.add(this.btnChatToggle);
    }
    
	
    @Override
    public void actionPerformed(GuiButton buttonPressed){
    	if (buttonPressed.id == BTN_CHAT_TOGGLE){
    		this.qtRef.toggleChat();
    	}
    	try {
			super.actionPerformed(buttonPressed);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
	
	@Override
    public void updateScreen(){
        super.updateScreen();
    }
	
    
	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks){
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (this.qtRef.isChatDisabled()){
			this.btnChatToggle.displayString = "Enable Chat";
		}
		else{
			this.btnChatToggle.displayString = "Disable Chat";
		}
	}
}
