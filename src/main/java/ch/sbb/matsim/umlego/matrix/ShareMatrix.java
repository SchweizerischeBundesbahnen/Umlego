package ch.sbb.matsim.umlego.matrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShareMatrix extends AbstractMatrix {

    private static final Logger LOG = LogManager.getLogger(ShareMatrix.class);

    private final String segment;

    public ShareMatrix(String segment, double[][] data) {
        super(data, segment);
        this.segment = segment;
    }

}
