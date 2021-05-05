package romever.scan.oasisscan.repository;

import romever.scan.oasisscan.entity.Delegator;

import java.util.Comparator;

public class SharesComparator implements Comparator<Delegator> {
    @Override
    public int compare(Delegator o1, Delegator o2) {
        return Long.compareUnsigned(Long.parseLong(o2.getShares()), Long.parseLong(o1.getShares()));
    }
}
