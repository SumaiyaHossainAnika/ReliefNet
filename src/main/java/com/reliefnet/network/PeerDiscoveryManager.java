package com.reliefnet.network;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;

/**
 * PeerDiscoveryManager - Handles discovery of nearby ReliefNet devices using mDNS
 * Allows devices to find each other even without internet connectivity
 */
public class PeerDiscoveryManager {
    
    private JmDNS jmdns;
    private final Map<String, PeerInfo> discoveredPeers = new ConcurrentHashMap<>();
    private ServiceInfo serviceInfo;
    private boolean isDiscovering = false;
    private String localServerAddress = null;
    
    public PeerDiscoveryManager() {
        try {
            // Get local IP address
            InetAddress localAddress = getLocalInetAddress();
            jmdns = JmDNS.create(localAddress);
            System.out.println("PeerDiscoveryManager initialized on " + localAddress.getHostAddress());
        } catch (IOException e) {
            System.err.println("Error initializing PeerDiscoveryManager: " + e.getMessage());
        }
    }
    
    /**
     * Start discovering nearby ReliefNet devices
     */
    public void startDiscovery() {
        if (isDiscovering || jmdns == null) {
            return;
        }
        
        try {
            isDiscovering = true;
            jmdns.addServiceListener(NetworkConfig.MESH_SERVICE_NAME, new ReliefNetServiceListener());
            System.out.println("Started peer discovery for service: " + NetworkConfig.MESH_SERVICE_NAME);
        } catch (Exception e) {
            System.err.println("Error starting peer discovery: " + e.getMessage());
            isDiscovering = false;
        }
    }
    
    /**
     * Stop peer discovery
     */
    public void stopDiscovery() {
        if (!isDiscovering || jmdns == null) {
            return;
        }
        
        try {
            isDiscovering = false;
            jmdns.removeServiceListener(NetworkConfig.MESH_SERVICE_NAME, new ReliefNetServiceListener());
            
            // Unregister our service if we're advertising
            if (serviceInfo != null) {
                jmdns.unregisterService(serviceInfo);
                serviceInfo = null;
            }
            
            System.out.println("Stopped peer discovery");
        } catch (Exception e) {
            System.err.println("Error stopping peer discovery: " + e.getMessage());
        }
    }
    
    /**
     * Advertise this device as a ReliefNet server (for authorities)
     */
    public void advertiseAsServer() {
        if (jmdns == null) {
            return;
        }
        
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String serviceName = "ReliefNet-" + hostname;
            
            serviceInfo = ServiceInfo.create(
                NetworkConfig.MESH_SERVICE_NAME,
                serviceName,
                NetworkConfig.WEBSOCKET_PORT,
                "ReliefNet Emergency Server"
            );
            
            jmdns.registerService(serviceInfo);
            localServerAddress = getLocalInetAddress().getHostAddress();
            
            System.out.println("Advertising as ReliefNet server: " + serviceName + " on " + localServerAddress);
            
        } catch (Exception e) {
            System.err.println("Error advertising as server: " + e.getMessage());
        }
    }
    
    /**
     * Find a local ReliefNet server
     */
    public String findLocalServer() {
        if (localServerAddress != null) {
            return localServerAddress;
        }
        
        // Look for authority servers in discovered peers
        for (PeerInfo peer : discoveredPeers.values()) {
            if (peer.isServer()) {
                return peer.getAddress();
            }
        }
        
        return null;
    }
    
    /**
     * Check if there are any nearby peers
     */
    public boolean hasNearbyPeers() {
        return !discoveredPeers.isEmpty();
    }
    
    /**
     * Get count of discovered peers
     */
    public int getPeerCount() {
        return discoveredPeers.size();
    }
    
    /**
     * Get all discovered peers
     */
    public Set<PeerInfo> getDiscoveredPeers() {
        return Set.copyOf(discoveredPeers.values());
    }
    
    private InetAddress getLocalInetAddress() throws IOException {
        try {
            // Try to find the best local address
            for (NetworkInterface networkInterface : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress addr = interfaceAddress.getAddress();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr;
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error getting network interfaces: " + e.getMessage());
        }
        
        // Fallback to localhost
        return InetAddress.getLocalHost();
    }
    
    public void shutdown() {
        try {
            stopDiscovery();
            if (jmdns != null) {
                jmdns.close();
            }
            System.out.println("PeerDiscoveryManager shutdown complete");
        } catch (Exception e) {
            System.err.println("Error during PeerDiscoveryManager shutdown: " + e.getMessage());
        }
    }
    
    /**
     * Service listener for ReliefNet mDNS services
     */
    private class ReliefNetServiceListener implements ServiceListener {
        
        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("ReliefNet service detected: " + event.getName());
            // Request service info
            jmdns.requestServiceInfo(event.getType(), event.getName(), 1000);
        }
        
        @Override
        public void serviceRemoved(ServiceEvent event) {
            String serviceName = event.getName();
            discoveredPeers.remove(serviceName);
            System.out.println("ReliefNet service removed: " + serviceName);
        }
        
        @Override
        public void serviceResolved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            String address = info.getInet4Addresses()[0].getHostAddress();
            int port = info.getPort();
            String name = info.getName();
            
            PeerInfo peer = new PeerInfo(name, address, port, info.getNiceTextString().contains("Server"));
            discoveredPeers.put(name, peer);
            
            System.out.println("ReliefNet service resolved: " + name + " at " + address + ":" + port);
        }
    }
    
    /**
     * Information about a discovered peer
     */
    public static class PeerInfo {
        private final String name;
        private final String address;
        private final int port;
        private final boolean isServer;
        private final long discoveredAt;
        
        public PeerInfo(String name, String address, int port, boolean isServer) {
            this.name = name;
            this.address = address;
            this.port = port;
            this.isServer = isServer;
            this.discoveredAt = System.currentTimeMillis();
        }
        
        public String getName() { return name; }
        public String getAddress() { return address; }
        public int getPort() { return port; }
        public boolean isServer() { return isServer; }
        public long getDiscoveredAt() { return discoveredAt; }
        
        @Override
        public String toString() {
            return String.format("PeerInfo{name='%s', address='%s', port=%d, isServer=%s}", 
                               name, address, port, isServer);
        }
    }
}
