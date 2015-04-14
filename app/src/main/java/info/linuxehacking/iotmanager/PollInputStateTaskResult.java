package info.linuxehacking.iotmanager;

import java.util.HashMap;

/**
 * Created by tiziano on 13/04/15.
 */
public class PollInputStateTaskResult {
    public HashMap<Integer,Boolean> digitalInState;
    public HashMap<Integer,Float> analogInState;
}
