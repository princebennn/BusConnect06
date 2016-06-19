package com.zegaout.BusConnect06;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by princeben on 20/02/2016.
 */
public class SearchAdapter extends ArrayAdapter<BusStop> {

    ArrayList<BusStop> _items;
    ArrayList<BusStop> _filtered;
    SearchFilter _filter;

    public SearchAdapter(Context context, ArrayList<BusStop> items) {
        super(context, 0, items);
        _filtered = items;
        _items = (ArrayList<BusStop>) items.clone();
        _filter = new SearchFilter();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        BusStop stop = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_elem, parent, false);
        }
        // Lookup view for data population
        TextView Name = (TextView) convertView.findViewById(R.id.name);
        TextView Num = (TextView) convertView.findViewById(R.id.num);
        // Populate the data into the template view using the data object
        Num.setText("" + stop._num);
        Name.setText(stop._name);
        // Return the completed view to render on screen
        return convertView;
    }

    @Override
    public Filter getFilter()
    {
        if(_filter == null)
            _filter = new SearchFilter();
        return _filter;
    }

    private class SearchFilter extends Filter
    {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            // NOTE: this function is *always* called from a background thread, and
            // not the UI thread.
            constraint = constraint.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if(constraint != null && constraint.toString().length() > 0)
            {
                ArrayList<BusStop> filt = new ArrayList<BusStop>();
                ArrayList<BusStop> lItems = new ArrayList<BusStop>();
                synchronized (this)
                {
                    lItems.addAll(_items);
                }
                for(int i = 0, l = lItems.size(); i < l; i++)
                {
                    BusStop m = lItems.get(i);
                    if(m._search_name.contains(constraint))
                        filt.add(m);
                    else if(m._num.toString().contains(constraint))
                        filt.add(m);
                }
                result.count = filt.size();
                result.values = filt;
            }
            else
            {
                result.values = _items.clone();
                result.count = _items.size();
            }
            return result;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            _filtered = (ArrayList<BusStop>)filterResults.values;
            notifyDataSetChanged();
            clear();
            for(int i = 0, l = _filtered.size(); i < l; i++)
                add(_filtered.get(i));
        }
    }
}
