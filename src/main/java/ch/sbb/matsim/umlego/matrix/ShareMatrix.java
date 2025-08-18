package ch.sbb.matsim.umlego.matrix;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
public class ShareMatrix extends AbstractMatrix {

    private static final Logger LOG = LogManager.getLogger(ShareMatrix.class);

    private final String segment;

    public ShareMatrix(String segment, double[][] data) {
        super(data, segment);
        this.segment = segment;
    }

}
