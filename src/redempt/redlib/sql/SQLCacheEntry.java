package redempt.redlib.sql;

import java.util.Arrays;
import java.util.Objects;

class SQLCacheEntry {

    private Object[] params;

    public SQLCacheEntry(Object[] params) {
        this.params = params;
    }

    public Object[] getParams() {
        return params;
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SQLCacheEntry)) {
            return false;
        }
        return Arrays.equals(params, ((SQLCacheEntry) o).params);
    }

}
