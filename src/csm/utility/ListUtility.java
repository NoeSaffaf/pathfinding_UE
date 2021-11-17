package csm.utility;

import csm.model.search.Node;

import java.util.ArrayList;
import java.util.List;

public class ListUtility {
    public static List<String> getURIListFromNodeList(List<Node> listNode)
    {
        List<String> uriList = new ArrayList<>();
        for (Node node : listNode)
        {
            uriList.add(node.getURI());
        }
        return uriList;
    }
}
