package kor.toxicity.cutscenemaker.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import net.milkbowl.vault.economy.Economy;

public final class MoneyUtil{

	private MoneyUtil() {
		throw new RuntimeException();
	}
	private static final Economy economy = Bukkit.getServicesManager().getRegistration(Economy.class) != null ? Bukkit.getServicesManager().getRegistration(Economy.class).getProvider() : null;

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
