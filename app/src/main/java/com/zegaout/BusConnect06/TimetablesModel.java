package com.zegaout.BusConnect06;

import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by princeben on 20/02/2016.
 */
public class TimetablesModel {

    public String  _stop;
    public String  _getTime;
    public ArrayList<LineTimetables>    _timetables = new ArrayList<LineTimetables>();

    public Boolean getFromHTML(Integer stop_id)
    {
        Document doc = null;
        Elements tmp = null;

        _getTime = null;
        try {
            doc = Jsoup.connect("http://cg06.tsi.cityway.fr/qrcode/?id=" + stop_id.toString()).get();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        //Get Stop Name
        tmp = doc.getElementsByClass("here");
        if (!tmp.isEmpty()) {
            tmp = tmp.get(0).getElementsByClass("txtbold");
            if (!tmp.isEmpty())
                _stop = tmp.get(0).text();
            else
                return false;
        } else {
            return false;
        }

        //Check if no Timetables
        if (doc.getElementsByClass("error").size() > 0)
            return false;

        //Get Time
        tmp = doc.getElementsByClass("infos");
        if (!tmp.isEmpty()) {
            tmp = tmp.get(0).getElementsByClass("txtbold");
            if (!tmp.isEmpty())
                _getTime = "à " + tmp.get(1).text();
            else
                return false;
        } else {
            return false;
        }

        for (Element elem : doc.getElementsByClass("data"))
        {
            LineTimetables lineTimetable = new LineTimetables();

            tmp = elem.getElementsByClass("txtbold");
            if (!tmp.isEmpty())
                tmp.get(0).remove();
            else
                return false;

            tmp = elem.getElementsByTag("div");
            if (!tmp.isEmpty())
                tmp = tmp.get(0).getElementsByClass("txtbold");
            else
                return false;

            for (int j = 0; j < tmp.size(); ++j)
            {
                Timetable timetable = new Timetable();
                timetable._time = tmp.get(j).text();
                if (StringUtil.isNumeric(timetable._time)) {
                    timetable._time += " min";
                    j++;
                } else if (!timetable._time.contains("h")) {
                    timetable._time = "à l'approche";
                } else {
                    j++;
                }
                timetable._direction = "-> " + tmp.get(j).text();
                lineTimetable._timetables.add(timetable);
            }
            String[] strs = elem.getElementsByTag("div").get(1).html().split("<br>");
            for (int j = 0; j < strs.length; ++j)
            {
                if (strs[j].contains("*"))
                    lineTimetable._timetables.get(j)._isVirtual = true;
            }
            elem.getElementsByTag("div").get(1).remove();
            lineTimetable._line = "Ligne " + elem.text().replaceAll(" ", "").replaceAll(":", "");
            _timetables.add(lineTimetable);
        }
        return true;
    }

    public class LineTimetables
    {
        public ArrayList<Timetable> _timetables = new ArrayList<Timetable>();
        public String      _line;
    }

    public class Timetable
    {
        public String  _time;
        public String  _direction;
        public Boolean _isVirtual = false;
    }
}
