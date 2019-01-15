package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class Wired extends Network {

    public Wired(Context context) {
        super(context);
    }

    /**
     * Reads the ethernet MAC address from the system.
     *
     * @return MAC address.
     * @throws IOException
     */
    @Override
    String getMacAddress() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader("/sys/class/net/eth0/address"))) {
            String address = br.readLine();
            return address.substring(0, 17);
        }
    }

    /**
     * Gets the subnet of the ethernet interface.
     *
     * @return Subnet.
     * @throws SubnetNotFoundException
     * @throws NoConnectivityManagerException
     */
    @Override
    int getSubnet() throws SubnetNotFoundException, NoConnectivityManagerException {
        ConnectivityManager cm = getConnectivityManager();
        List<LinkAddress> addresses = cm.getLinkProperties(cm.getActiveNetwork()).getLinkAddresses();
        for (LinkAddress address : addresses) {
            InetAddress addr = address.getAddress();
            if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                return address.getPrefixLength();
            }
        }

        throw new SubnetNotFoundException();
    }

}
