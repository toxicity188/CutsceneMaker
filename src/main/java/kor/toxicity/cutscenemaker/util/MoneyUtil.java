package kor.toxicity.cutscenemaker.util;

import java.text.DecimalFormat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import net.milkbowl.vault.economy.Economy;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MoneyUtil{
	@Getter
	private static final MoneyUtil instance = new MoneyUtil();
	private final Economy economy = Bukkit.getServicesManager().getRegistration(Economy.class) != null ? Bukkit.getServicesManager().getRegistration(Economy.class).getProvider() : null;

	public double getMoney(OfflinePlayer p) {
		if (economy != null) return economy.getBalance(p);
		return 0;
	}
	public void addMoney(OfflinePlayer p, double money) {
		if (economy == null) return;
		if (!economy.hasAccount(p)) economy.createPlayerAccount(p);
		economy.depositPlayer(p, money);
	}
	public void removeMoney(OfflinePlayer p, double money) {
		if (economy == null) return;
		if (!economy.hasAccount(p)) economy.createPlayerAccount(p);
		economy.withdrawPlayer(p, money);
	}

}
