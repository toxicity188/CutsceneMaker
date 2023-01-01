package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class ActLeap extends CutsceneAction {

	@DataField(aliases = "a")
	public int amount = 8;
	@DataField(aliases = "r")
	public double rotation = 0;
	@DataField(aliases = "f")
	public double forward = 15;
	@DataField(aliases = "u")
	public double upward = 3;
	@DataField(aliases = "m")
	public boolean movement = true;

	private final CutsceneManager manager;

	public ActLeap(CutsceneManager pl) {
		super(pl);
		manager = pl;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (rotation < 1) rotation = 1;
	}

	@Override
	protected void apply(LivingEntity entity) {
		if (movement) {
			Location location = entity.getLocation();
			double x = location.getX();
			double z = location.getZ();
			double yaw = location.getYaw();
			manager.runTaskLater(() -> {
				Location location1 = entity.getLocation();
				double degree = Math.atan2(location1.getZ() - z, location1.getX() - x);
				if (degree != 0) {
					degree = degree *180/Math.PI - 90;
					if (degree < 0) degree += 360;
					degree -= yaw;
				}
				leap(entity,rotation + (float) Math.round(degree/amount) * amount);
			}, 1);

		} else leap(entity,rotation);
	}
	private void leap(LivingEntity player, double rot) {
		Vector v = player.getLocation().getDirection();
		v.setY(0).normalize().multiply(forward).setY(upward);
		if (rot != 0) rotateYaw(v, rot);
		player.setVelocity(v);
	}

	public void rotateYaw(Vector target, double yaw) {
		double x = -target.getX();
		double z = target.getZ();
		double t = Math.toRadians(yaw);
		target.setX(- z*Math.sin(t) - x*Math.sin(t+Math.PI/2)).setZ(z*Math.cos(t) + x*Math.cos(t+Math.PI/2));
	}

}
