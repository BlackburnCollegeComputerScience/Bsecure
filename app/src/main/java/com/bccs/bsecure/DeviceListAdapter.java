package com.bccs.bsecure;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 *
 * This file is part of Bsecure. A open source, freely available, SMS encryption app.
 * Copyright (C) 2015 Dr Kevin Coogan, Shane Nalezyty, Lucas Burdell
 *
 * Bsecure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bsecure is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bsecure.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

public class DeviceListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<BluetoothDevice> data;
    private OnPairButtonClickListener pairListener;
    private OnConnectButtonClickListener connectListener;


    public DeviceListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setData(List<BluetoothDevice> data) {
        this.data = data;
    }

    public void setPairListener(OnPairButtonClickListener pairListener) {
        this.pairListener = pairListener;
    }

    public void setConnectListener(OnConnectButtonClickListener connectListener) {
        this.connectListener = connectListener;
    }

    public int getCount() {
        return (data == null) ? 0 : data.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * Implementation of a adapters abstract method getView. Returns the view of a single position
     * in the data set
     */
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        //ConvertView is a view that is being hidden off screen so we can reuse it instead of
        //making needless views

        if (convertView == null) {
            //if convertView is null no item is being hidden off screen so we need to make a new one

            //Use the layout inflater to instantiate a XML file to be used for a view
            convertView = inflater.inflate(R.layout.list_item_device, null);

            holder = new ViewHolder();
            //Apply the layout from the inflater to a new view called holder
            holder.nameTv = (TextView) convertView.findViewById(R.id.tv_name);
            holder.addressTv = (TextView) convertView.findViewById(R.id.tv_address);
            holder.pairBtn = (Button) convertView.findViewById(R.id.btn_pair);
            holder.connectBtn = (Button) convertView.findViewById(R.id.btn_connect);

            //Adds the view to convertView as a tag
            convertView.setTag(holder);
        } else {
            //If convert view is not null we can just convert it into my view Viewholder
            holder = (ViewHolder) convertView.getTag();
        }
        //Get the bluetooth device at the specified position.
        BluetoothDevice device = data.get(position);
        //Set the view variables with the devices
        holder.nameTv.setText(device.getName());
        holder.addressTv.setText(device.getAddress());
        holder.pairBtn.setText((device.getBondState() == BluetoothDevice.BOND_BONDED) ? "Unpair" : "Pair");
        holder.pairBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pairListener != null) {
                    //Runs the listeners onPairButtonClick as specified in our interface.
                    pairListener.onPairButtonClick(position);
                }
            }
        });
        holder.connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectListener != null) {
                    connectListener.OnConnectButtonClick(position);
                }
            }
        });

        return convertView;
    }

    /**
     * Class to hold our view parameters together
     */
    static class ViewHolder {
        TextView nameTv;
        TextView addressTv;
        Button pairBtn;
        Button connectBtn;
    }

    /**
     * Interface for pair button pairListener
     */
    public interface OnPairButtonClickListener {
        void onPairButtonClick(int position);
    }

    /**
     * Interface for connect button pairListener
     */
    public interface OnConnectButtonClickListener {
        void OnConnectButtonClick(int position);
    }
}
