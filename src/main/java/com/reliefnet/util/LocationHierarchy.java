package com.reliefnet.util;

import java.util.*;

/**
 * Utility class to manage the location hierarchy for Bangladesh
 * Maps districts to their parent divisions to support location-based filtering
 */
public class LocationHierarchy {
    
    // Map of districts to their parent divisions
    private static final Map<String, String> districtToDivision = new HashMap<>();
    
    static {
        // Dhaka Division
        addDistrictToDivision("Dhaka", "Dhaka");
        addDistrictToDivision("Gazipur", "Dhaka");
        addDistrictToDivision("Narsingdi", "Dhaka");
        addDistrictToDivision("Manikganj", "Dhaka");
        addDistrictToDivision("Munshiganj", "Dhaka");
        addDistrictToDivision("Narayanganj", "Dhaka");
        addDistrictToDivision("Tangail", "Dhaka");
        addDistrictToDivision("Kishoreganj", "Dhaka");
        addDistrictToDivision("Madaripur", "Dhaka");
        addDistrictToDivision("Rajbari", "Dhaka");
        addDistrictToDivision("Gopalganj", "Dhaka");
        addDistrictToDivision("Faridpur", "Dhaka");
        addDistrictToDivision("Shariatpur", "Dhaka");
        
        // Chittagong Division
        addDistrictToDivision("Chittagong", "Chittagong");
        addDistrictToDivision("Cox's Bazar", "Chittagong");
        addDistrictToDivision("Rangamati", "Chittagong");
        addDistrictToDivision("Bandarban", "Chittagong");
        addDistrictToDivision("Khagrachari", "Chittagong");
        addDistrictToDivision("Feni", "Chittagong");
        addDistrictToDivision("Lakshmipur", "Chittagong");
        addDistrictToDivision("Noakhali", "Chittagong");
        addDistrictToDivision("Brahmanbaria", "Chittagong");
        addDistrictToDivision("Comilla", "Chittagong");
        addDistrictToDivision("Chandpur", "Chittagong");
        
        // Rajshahi Division
        addDistrictToDivision("Rajshahi", "Rajshahi");
        addDistrictToDivision("Natore", "Rajshahi");
        addDistrictToDivision("Naogaon", "Rajshahi");
        addDistrictToDivision("Chapai Nawabganj", "Rajshahi");
        addDistrictToDivision("Pabna", "Rajshahi");
        addDistrictToDivision("Bogra", "Rajshahi");
        addDistrictToDivision("Sirajganj", "Rajshahi");
        addDistrictToDivision("Joypurhat", "Rajshahi");
        
        // Khulna Division
        addDistrictToDivision("Khulna", "Khulna");
        addDistrictToDivision("Jessore", "Khulna");
        addDistrictToDivision("Satkhira", "Khulna");
        addDistrictToDivision("Bagerhat", "Khulna");
        addDistrictToDivision("Narail", "Khulna");
        addDistrictToDivision("Chuadanga", "Khulna");
        addDistrictToDivision("Meherpur", "Khulna");
        addDistrictToDivision("Kushtia", "Khulna");
        addDistrictToDivision("Jhenaidah", "Khulna");
        addDistrictToDivision("Magura", "Khulna");
        
        // Barisal Division
        addDistrictToDivision("Barisal", "Barisal");
        addDistrictToDivision("Bhola", "Barisal");
        addDistrictToDivision("Jhalokati", "Barisal");
        addDistrictToDivision("Patuakhali", "Barisal");
        addDistrictToDivision("Pirojpur", "Barisal");
        addDistrictToDivision("Barguna", "Barisal");
        
        // Sylhet Division
        addDistrictToDivision("Sylhet", "Sylhet");
        addDistrictToDivision("Moulvibazar", "Sylhet");
        addDistrictToDivision("Habiganj", "Sylhet");
        addDistrictToDivision("Sunamganj", "Sylhet");
        
        // Rangpur Division
        addDistrictToDivision("Rangpur", "Rangpur");
        addDistrictToDivision("Dinajpur", "Rangpur");
        addDistrictToDivision("Kurigram", "Rangpur");
        addDistrictToDivision("Gaibandha", "Rangpur");
        addDistrictToDivision("Nilphamari", "Rangpur");
        addDistrictToDivision("Panchagarh", "Rangpur");
        addDistrictToDivision("Thakurgaon", "Rangpur");
        addDistrictToDivision("Lalmonirhat", "Rangpur");
        
        // Mymensingh Division
        addDistrictToDivision("Mymensingh", "Mymensingh");
        addDistrictToDivision("Jamalpur", "Mymensingh");
        addDistrictToDivision("Netrokona", "Mymensingh");
        addDistrictToDivision("Sherpur", "Mymensingh");
    }
      private static void addDistrictToDivision(String district, String division) {
        districtToDivision.put(district.toLowerCase(), division);
    }
    
    /**
     * For debugging - print the full location map
     */
    public static void printLocationMap() {
        System.out.println("DEBUG: Location Hierarchy Map:");
        for (Map.Entry<String, String> entry : districtToDivision.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
    }
    
    /**
     * Get the division for a given district
     * @param district The district to look up
     * @return The parent division or the district itself if not found
     */
    public static String getDivisionForDistrict(String district) {
        if (district == null || district.trim().isEmpty()) {
            return "";
        }
        
        String normalizedDistrict = district.toLowerCase().trim();
        return districtToDivision.getOrDefault(normalizedDistrict, district);
    }
    
    /**
     * Check if a location (volunteer location) is in the specified target location
     * This handles the hierarchy: if target is a division, volunteers from any district in that division match
     * @param volunteerLocation The location of the volunteer
     * @param targetLocation The location we're searching for volunteers in
     * @return true if the volunteer location is in or matches the target location
     */
    public static boolean isLocationMatch(String volunteerLocation, String targetLocation) {
        if (volunteerLocation == null || targetLocation == null) {
            return false;
        }
        
        // Normalize both locations
        String normVolunteerLoc = volunteerLocation.toLowerCase().trim();
        String normTargetLoc = targetLocation.toLowerCase().trim();
        
        // Direct match
        if (normVolunteerLoc.equals(normTargetLoc)) {
            return true;
        }
        
        // Check if volunteer is from the division we're targeting
        if (districtToDivision.containsKey(normTargetLoc)) {
            // Target is a district, check if volunteer is from the same division
            String targetDivision = districtToDivision.get(normTargetLoc);
            String volunteerDivision = getDivisionForDistrict(volunteerLocation);
            return targetDivision.equalsIgnoreCase(volunteerDivision);
        } else {
            // Target might be a division, check if volunteer district belongs to this division
            return normTargetLoc.equals(getDivisionForDistrict(volunteerLocation).toLowerCase());
        }
    }
    
    /**
     * Get all districts in a division
     * @param division The division name
     * @return List of districts in the division
     */
    public static List<String> getDistrictsInDivision(String division) {
        if (division == null || division.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String normDivision = division.trim();
        List<String> districts = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : districtToDivision.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(normDivision)) {
                districts.add(entry.getKey());
            }
        }
        
        return districts;
    }
    
    /**
     * Get all divisions
     * @return Set of all divisions
     */
    public static Set<String> getAllDivisions() {
        return new HashSet<>(districtToDivision.values());
    }
    
    /**
     * Get all districts
     * @return Set of all districts
     */
    public static Set<String> getAllDistricts() {
        return districtToDivision.keySet();
    }
}
