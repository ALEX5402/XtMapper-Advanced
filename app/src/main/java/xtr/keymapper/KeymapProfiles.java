package xtr.keymapper;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import xtr.keymapper.dpad.Dpad;
import xtr.keymapper.mouse.MouseAimConfig;

public class KeymapProfiles {
    private final SharedPreferences sharedPref;

    public KeymapProfiles(Context context) {
        sharedPref = context.getSharedPreferences("profiles", MODE_PRIVATE);
    }

    public Map<String, Profile> getAllProfiles(){
        Map<String, Profile> profiles = new HashMap<>();
        sharedPref.getAll().forEach((BiConsumer<String, Object>) (key, o) -> profiles.put(key, getProfile(key)));
        return profiles;
    }

    public void renameProfile(String profileName, String newProfile) {
        Set<String> stringSet = sharedPref.getStringSet(profileName, null);
        deleteProfile(profileName);
        sharedPref.edit()
                .putStringSet(newProfile, stringSet)
                .apply();
    }

    static final class Key {

        String code;
        float x;
        float y;
    }
     public static final class Profile {

         public String packageName = "xtr.keymapper";
         Dpad dpad1 = null;
         Dpad dpad2 = null;
         MouseAimConfig mouseAimConfig = null;
         ArrayList<Key> keys = new ArrayList<>();
     }
    public void saveProfile(String profile, ArrayList<String> lines, String packageName) {
        lines.add("APPLICATION " + packageName);
        Set<String> stringSet = new HashSet<>(lines);
        sharedPref.edit()
                .putStringSet(profile, stringSet)
                .apply();
    }

    public void deleteProfile(String profileName){
        sharedPref.edit().remove(profileName).apply();
    }

    public Profile getProfile(String profileName) {
        Set<String> stream = sharedPref.getStringSet(profileName, null);

        Profile profile = new Profile();
        if (stream != null) stream.forEach(s -> {

            String[] data = s.split("\\s+"); // Split a String like KEY_G 760.86346 426.18607
            switch (data[0]){
                case "UDLR_DPAD":
                    profile.dpad1 = new Dpad(data);
                    break;

                case "WASD_DPAD":
                    profile.dpad2 = new Dpad(data);
                    break;

                case "MOUSE_AIM":
                    profile.mouseAimConfig = new MouseAimConfig().parse(data);
                    break;

                case "APPLICATION":
                    profile.packageName = data[1];
                    break;

                default: {
                    final Key key = new Key();
                    key.code = data[0];
                    key.x = Float.parseFloat(data[1]);
                    key.y = Float.parseFloat(data[2]);
                    profile.keys.add(key);
                    break;
                }
            }
        });
        return profile;
    }
}