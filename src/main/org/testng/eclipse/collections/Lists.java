package org.testng.eclipse.collections;

import java.util.ArrayList;
import java.util.List;

public class Lists {

  public static <K> List<K> newArrayList() {
    return new ArrayList<K>();
  }
}
