package dolozimm.apoapsis;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Apoapsis implements ModInitializer {
	public static final String MOD_ID = "apoapsis";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Apoapsis initialized!");
	}
}