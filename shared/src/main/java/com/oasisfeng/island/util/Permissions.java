package com.oasisfeng.island.util;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.support.annotation.StringDef;

import com.oasisfeng.island.analytics.Analytics;

import java9.util.Optional;

import static android.Manifest.permission.PACKAGE_USAGE_STATS;
import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;
import static com.oasisfeng.android.Manifest.permission.INTERACT_ACROSS_USERS;

/**
 * Permission-related helpers
 *
 * Created by Oasis on 2017/10/8.
 */
public class Permissions {

	@TargetApi(M) @StringDef({ INTERACT_ACROSS_USERS, WRITE_SECURE_SETTINGS, PACKAGE_USAGE_STATS }) @interface DevPermission {}

	public static boolean has(final Context context, final String permission) {
		return context.checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean ensure(final Context context, final @DevPermission String permission) {
		if (has(context, permission)) return true;
		if (SDK_INT < M || SDK_INT > O) return false;
		final String sp = Build.VERSION.SECURITY_PATCH;
		if (sp.contains("2018") || sp.contains("2017-11") || sp.contains("2017-12")) return false;	// No longer works after 2017.11 security patch. (CVE-2017-0830)

		if (Users.isOwner() && ! new DevicePolicies(context).isDeviceOwner()) return false;
		if (Users.isProfile()) {
			final Optional<Boolean> is_owner = DevicePolicies.isProfileOwner(context);
			if (is_owner == null || ! is_owner.orElse(false)) return false;
		}
		final boolean result = new DevicePolicies(context).setPermissionGrantState(context.getPackageName(), permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
		if (! result) Analytics.$().event("permission_failure").withRaw("permission", permission).withRaw("SP", sp).send();
		return result;
	}
}