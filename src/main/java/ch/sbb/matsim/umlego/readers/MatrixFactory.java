package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.config.DemandMatrixParameter;
import ch.sbb.matsim.umlego.config.MatricesParameters;
import ch.sbb.matsim.umlego.config.ShareMatrixParameter;
import ch.sbb.matsim.umlego.matrix.AbstractMatrix;
import ch.sbb.matsim.umlego.matrix.DemandMatrix;
import ch.sbb.matsim.umlego.matrix.ShareMatrix;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;

public class MatrixFactory {

    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(MatrixFactory.class);
    private final MatricesParameters parameters;

    public MatrixFactory(MatricesParameters parameters) {
        this.parameters = parameters;
    }

    public Set<Integer> getNos() {
        var nos = this.parameters.demandMatrices().stream().map(DemandMatrixParameter::no).collect(Collectors.toSet());
        nos.addAll(this.parameters.shareMatrices().stream().map(ShareMatrixParameter::no).collect(Collectors.toSet()));
        return nos;
    }

    public AbstractMatrix createMatrix(Integer no, double[][] d) {

        var demandOpt = this.parameters.demandMatrices().stream().filter(m -> m.no().equals(no)).findFirst();
        var shareOpt = this.parameters.shareMatrices().stream().filter(m -> m.no().equals(no)).findFirst();

        if (demandOpt.isPresent()) {
            var param = demandOpt.get();
            return new DemandMatrix(param.startTimeInclusiveMin(), param.endTimeExclusiveMin(), d);

        } else if (shareOpt.isPresent()) {
            var param = shareOpt.get();
            return new ShareMatrix(param.segment(), d);
        } else {

            LOG.info("Matrix {} is not a demand or share matrix, skipping it", no);
            return null;
        }

    }
}
