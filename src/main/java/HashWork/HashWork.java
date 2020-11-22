package HashWork;

import java.text.NumberFormat;
import java.util.*;

public class HashWork {
    static String[] servers = {
            "192.168.0.1:100",
            "192.168.0.1:101",
            "192.168.0.1:102",
            "192.168.0.1:103",
            "192.168.0.1:104",
            "192.168.0.1:105",
            "192.168.0.1:106",
            "192.168.0.1:107",
            "192.168.0.1:108",
            "192.168.0.1:109",
            "192.168.0.1:110"
    };


    private static SortedMap<Integer, String> integerStringTreeMap = new TreeMap<Integer, String>();

    static {
        for (int i=0; i<servers.length; i++) {
            int hash = getHash(servers[i]);
            System.out.println("[" + servers[i] + "]Join, Hash is" + hash);
            integerStringTreeMap.put(hash, servers[i]);
        }
        System.out.println();
    }


    private static String getServer(String key) {
        int hash = getHash(key);
        SortedMap<Integer, String> subMap = integerStringTreeMap.tailMap(hash);
        if(subMap.isEmpty()){
            Integer i = integerStringTreeMap.firstKey();
            return integerStringTreeMap.get(i);
        }else{
            Integer i = subMap.firstKey();
            return subMap.get(i);
        }
    }
    private static int getHash(String str) {
        int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++)
            hash = (hash ^ str.charAt(i)) * p;
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        if (hash < 0)
            hash = Math.abs(hash);
        return hash;
    }
    public static void main(String[] args) {
        HashMap<String,Integer> serVerCountMap =new HashMap<>();
        for (String serverId :servers){
            serVerCountMap.put(serverId,0);
        }
        Random rd=new Random();
        for (int i=0 ;i<1000000;i++){
            String key=i+ "" +rd.nextGaussian();
            String serName = getServer(key);
            Integer val=Integer.parseUnsignedInt(serVerCountMap.get(serName).toString())+1;
            serVerCountMap.put(serName,val);
        }
        int total =0;
        for (String serverId :servers){
            int count =Integer.parseInt(serVerCountMap.get(serverId).toString());
            total+=count;

            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setMaximumFractionDigits(2);
            String result = numberFormat.format((float)  count/ (float)1000000* 100);//所占百分比


            System.out.printf("%s count is %s ,rate is %s \r\n ",serverId,serVerCountMap.get(serverId),result);
        }
        System.out.printf("Total count :%s\r\n",total);

    }
}
