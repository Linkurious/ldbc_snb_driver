package com.ldbc.driver.validation;

import com.ldbc.driver.*;
import com.ldbc.driver.generator.Generator;
import com.ldbc.driver.generator.GeneratorException;

import java.util.Iterator;

public class ValidationParamsGenerator extends Generator<ValidationParam> {
    private final Db db;
    private final Workload.DbValidationParametersFilter dbValidationParametersFilter;
    private final Iterator<Operation<?>> operations;
    private int entriesWrittenSoFar;
    private boolean needMoreValidationParameters;

    public ValidationParamsGenerator(Db db,
                                     Workload.DbValidationParametersFilter dbValidationParametersFilter,
                                     Iterator<Operation<?>> operations) {
        this.db = db;
        this.dbValidationParametersFilter = dbValidationParametersFilter;
        this.operations = operations;
        this.entriesWrittenSoFar = 0;
        this.needMoreValidationParameters = true;
    }

    public int entriesWrittenSoFar() {
        return entriesWrittenSoFar;
    }

    @Override
    protected ValidationParam doNext() throws GeneratorException {
        while (operations.hasNext() && needMoreValidationParameters) {
            Operation<?> operation = operations.next();

            if (false == dbValidationParametersFilter.useOperation(operation))
                continue;

            OperationHandler<Operation<?>> handler;
            try {
                handler = (OperationHandler<Operation<?>>) db.getOperationHandler(operation);
            } catch (DbException e) {
                throw new GeneratorException(
                        String.format(""
                                        + "Error retrieving operation handler for operation\n"
                                        + "Db: %s\n"
                                        + "Operation: %s",
                                db.getClass().getName(), operation),
                        e);
            }
            OperationResultReport operationResultReport;
            try {
                operationResultReport = handler.executeOperationUnsafe(operation);
            } catch (DbException e) {
                throw new GeneratorException(
                        String.format(""
                                        + "Error executing operation to retrieve validation result\n"
                                        + "Db: %s\n"
                                        + "Operation: %s",
                                db.getClass().getName(), operation),
                        e);
            }
            Object operationResult = operationResultReport.operationResult();

            switch (dbValidationParametersFilter.useOperationAndResultForValidation(operation, operationResult)) {
                case REJECT_AND_CONTINUE:
                    continue;
                case REJECT_AND_FINISH:
                    needMoreValidationParameters = false;
                    continue;
                case ACCEPT_AND_CONTINUE:
                    entriesWrittenSoFar++;
                    return new ValidationParam(operation, operationResult);
                case ACCEPT_AND_FINISH:
                    entriesWrittenSoFar++;
                    needMoreValidationParameters = false;
                    return new ValidationParam(operation, operationResult);
            }
        }
        // ran out of operations OR validation set size has been reached
        return null;
    }
}