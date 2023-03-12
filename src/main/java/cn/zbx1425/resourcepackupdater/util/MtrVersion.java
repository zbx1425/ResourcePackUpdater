package cn.zbx1425.resourcepackupdater.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MtrVersion implements Comparable<MtrVersion> {

    public final int[] parts;

    private MtrVersion(int[] parts) {
        this.parts = parts;
    }

    public static MtrVersion parse(String src) {
        ArrayList<Integer> parts = new ArrayList<>();
        String[] strParts = src.split("-");
        for (int i = 1; i < strParts.length; i++) {
            String[] subParts = strParts[i].split("\\.");
            for (String subPart : subParts) {
                if (subPart.matches("\\d+")) {
                    parts.add(Integer.parseInt(subPart));
                }
            }
        }
        return new MtrVersion(parts.stream().mapToInt(i -> i).toArray());
    }

    @Override
    public int compareTo(@NotNull MtrVersion o) {
        for (int i = 0; i < Math.min(this.parts.length, o.parts.length); i++) {
            if (this.parts[i] != o.parts[i]) {
                return Integer.compare(this.parts[i], o.parts[i]);
            }
        }
        return Integer.compare(this.parts.length, o.parts.length);
    }

    public boolean matches(String criteria) {
        if (criteria.isEmpty()) return false;
        switch (criteria.charAt(0)) {
            case '=':
                return this.compareTo(MtrVersion.parse(criteria.substring(1))) == 0;
            case '>':
                return this.compareTo(MtrVersion.parse(criteria.substring(1))) > 0;
            case '<':
                return this.compareTo(MtrVersion.parse(criteria.substring(1))) < 0;
            default:
                return this.compareTo(MtrVersion.parse(criteria.substring(1))) >= 0;
        }
    }
}
