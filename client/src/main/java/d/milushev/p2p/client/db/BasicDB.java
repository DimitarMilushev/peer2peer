package main.java.d.milushev.p2p.client.db;


import main.java.d.milushev.p2p.client.db.exceptions.InternalDatabaseException;
import main.java.d.milushev.p2p.client.db.exceptions.TableCreationException;
import main.java.d.milushev.p2p.client.db.exceptions.TableDeletionException;
import main.java.d.milushev.p2p.client.db.exceptions.TableModifyException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A simple database that manages tables using csv files. It's a small combo of ORM and a DB connector. This version is not thread-safe.
 */
public class BasicDB
{
    private static final Logger LOG = LogManager.getLogger(BasicDB.class);

    private static final String TABLE_FILE_SUFFIX = ".csv";
    private static final String CSV_SEPARATOR = ";";

    // Helps manage the properties of each table.
    private Map<String, Set<String>> propertiesPerTable;

    private final Path workingDir;


    public BasicDB(String workingDir)
    {
        this.workingDir = Path.of(workingDir);
        this.propertiesPerTable = new HashMap<>();
    }


    /**
     * Works specifically with the Java records api.
     *
     * @throws TableModifyException If the table does not exist or is inaccessible.
     */
    public void addRecords(String table, Class<?> type, Set<Object> records)
                    throws TableModifyException
    {
        final Set<String> properties = propertiesPerTable.get(table);
        if (properties == null)
        {
            throw new TableModifyException("Table " + table + " does not exist");
        }

        List<Method> fieldGetters = new ArrayList<>(properties.size());
        for (var propertyName : properties)
        {
            try
            {
                fieldGetters.add(type.getDeclaredMethod(propertyName));
            }
            catch (NoSuchMethodException e)
            {
                throw new TableModifyException("No field " + propertyName + " present", e);
            }
        }

        try
        {
            final List<String> recordLines = mapRecordsData(records, fieldGetters);
            final File tableFile = getTablePathWithDataType(table).toFile();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true)))
            {
                for (var dataLine : recordLines)
                {
                    writer.write(dataLine);
                    writer.newLine();
                }
            }
            catch (IOException e)
            {
                throw new InternalDatabaseException("Failed to write to table " + table, e);
            }
        }
        catch (InvocationTargetException | IllegalAccessException e)
        {
            throw new TableModifyException("Failed to map records data");
        }
    }


    private List<String> mapRecordsData(Set<Object> records, List<Method> getters)
                    throws InvocationTargetException, IllegalAccessException
    {
        final List<String> results = new ArrayList<>(records.size());

        for (final Object data : records)
        {
            final StringBuilder sb = new StringBuilder();

            for (var method : getters)
            {
                final String value = String.valueOf(method.invoke(data));
                sb.append(value);

                // Skip the last separator
                if (method != getters.getLast())
                {
                    sb.append(CSV_SEPARATOR);
                }
            }

            results.add(sb.toString());
        }

        return results;
    }


    public void createTable(String name, Set<String> properties)
                    throws TableCreationException
    {
        LOG.info("Creating table {} with properties {}", name, properties);
        final Path tablePath = getTablePathWithDataType(name);
        final File tableFile = createTableFile(tablePath);

        fillTableProperties(tableFile, properties);
        propertiesPerTable.put(name, properties);
    }


    public void removeTable(String name)
                    throws TableDeletionException
    {
        final Path tablePath = getTablePathWithDataType(name);

        try
        {
            Files.delete(tablePath);
            propertiesPerTable.remove(name);
        }
        catch (NoSuchFileException e)
        {
            throw new TableDeletionException("Table " + tablePath.getFileName() + " does not exist", e);
        }
        catch (IOException e)
        {
            throw new InternalDatabaseException("Unexpected IO error when deleting table " + tablePath.getFileName(), e);
        }
    }


    /**
     * Fills the table with the properties in CSV format.
     *
     * @param tableFile Target table file.
     * @param properties The set of properties.
     * @throws InternalDatabaseException in case of a file issue.
     */
    private void fillTableProperties(File tableFile, Set<String> properties)
    {
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile)))
        {
            if (Files.size(tableFile.toPath()) != 0)
            {
                throw new InternalDatabaseException("Failed to append properties due to malformed table");
            }
            final String csvEncodedProperties = String.join(CSV_SEPARATOR, properties);

            writer.write(csvEncodedProperties);
            writer.newLine();
        }
        catch (FileNotFoundException e)
        {
            throw new InternalDatabaseException("Table not present after creation", e);
        }
        catch (IOException e)
        {
            throw new InternalDatabaseException("The table path is malformed or points to a directory", e);
        }
    }


    private File createTableFile(Path tablePath)
                    throws TableCreationException
    {
        LOG.info("Creating table at {}", tablePath);
        final File tableFile = tablePath.toFile();

        try
        {
            final boolean fileCreated = tableFile.createNewFile();
            if (!fileCreated)
            {
                throw new TableCreationException("Table " + tablePath.getFileName() + " already exists.");
            }
        }
        catch (IOException e)
        {
            throw new TableCreationException("Unexpected IO error when creating table " + tablePath.getFileName(), e);
        }

        return tableFile;
    }


    private Path getTablePathWithDataType(String name)
    {
        return workingDir.resolve(name + TABLE_FILE_SUFFIX);
    }
}
