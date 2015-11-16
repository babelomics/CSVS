package org.babelomics.csvs.lib.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryResponse<T> {

    private static final long serialVersionUID = -2978952531219554024L;

    private Error error;
    private long numResults;
    private long numTotalResults;

    private Map<String, Object> queryOptions;

    private List<T> result;


    public QueryResponse() {
        this.queryOptions = new HashMap<>();
        this.result = new ArrayList<>();
        this.error = new Error();
    }


    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public long getNumResults() {
        return numResults;
    }

    public void setNumResults(long numResults) {
        this.numResults = numResults;
    }

    public long getNumTotalResults() {
        return numTotalResults;
    }

    public void setNumTotalResults(long numTotalResults) {
        this.numTotalResults = numTotalResults;
    }

    public Map<String, Object> getQueryOptions() {
        return queryOptions;
    }

    public void setQueryOptions(Map<String, Object> queryOptions) {
        this.queryOptions = queryOptions;
    }

    public List<T> getResult() {
        return result;
    }

    public void setResult(List<T> result) {
        this.numResults = result.size();
        this.result = result;
    }

    public void addQueryOption(String key, Object value) {
        this.queryOptions.put(key, value);
    }

    class Error {
        private String msg;
        private int code;

        public Error(String msg, int code) {
            this.msg = msg;
            this.code = code;
        }

        public Error() {
            this("", -1);
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }


    //{
//    error: {
//        msg: "",
//                code: 0
//    },
//    numResults: x,
//    numTotalResults: y
//    queryOptions: {
//        exclude: null,
//                include: null,
//                metadata: null
//    },
//    result: [
//            [ { dato1.1 }, { dato1.2 } ],
//            [ { dato2.1 }, { dato2.2 } ],
//            [],
//            null
//            ]
//}
}
