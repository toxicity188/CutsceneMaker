package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class MoneyUtil{

	private MoneyUtil() {
		throw new RuntimeException();
	}
	private static final Economy economy = setup();

	private static Economy setup() {
		try {
			RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
			return provider != null ? provider.getProvider() : null;
		} catch (Throwable t) {
			CutsceneMaker.warn("Vault not found!");
		}
		return null;
	}

	public static double getMoney(OfflinePlayer p) {
		if (economy != null) return economy.getBalance(p);
		return 0;
	}
	public static void addMoney(OfflinePlayer p, double money) {
		if (economy == null) return;
		if (!economy.hasAccount(p)) economy.createPlayerAccount(p);
		economy.depositPlayer(p, money);
	}
	public static void removeMoney(OfflinePlayer p, double money) {
		if (economy == null) return;
		if (!economy.hasAccount(p)) economy.createPlayerAccount(p);
		economy.withdrawPlayer(p, money);
	}

	public static boolean hasVault() {
		return economy != null;
	}

}
