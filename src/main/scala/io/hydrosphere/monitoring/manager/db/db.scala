package io.hydrosphere.monitoring.manager

import java.io.Closeable
import javax.sql.DataSource

package object db {
  type CloseableDataSource = DataSource with Closeable
}
