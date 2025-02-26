package nigloo.tool.rpa;

import net.razorvine.pickle.Pickler;
import net.razorvine.pickle.Unpickler;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


public class RenPyArchive implements Closeable
{
    public static void main(String[] args)
    {
        try {
            Path file = Path.of("D:\\Documents\\.med_rev\\ssdssd\\Jeux\\Steam\\steamapps\\common\\Sakura Succubus\\game\\assets.rpa");
            Path output = Path.of("C:\\Users\\sebas\\git\\GalleryManager\\target\\rpa_out");

            RenPyArchive archive = new RenPyArchive(file, null, null, null, true);

            List<Path> files = archive.list();

            Files.createDirectories(output);


            for (Path filename :files)
            {
                try
                {
                    byte[] contents = archive.read(filename);

                    Path filepath = output.resolve(filename);
                    Files.createDirectories(filepath.getParent());

                    Files.write(filepath, contents);
                }
                catch (Exception e) {
                    System.err.println("Could not extract file "+filename+" from archive");
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public record Version(int major, int minor) {
        public static Version V1 = new Version(1,0);
        public static Version V2 = new Version(2,0);
        public static Version V3 = new Version(3,0);
        public static Version V3_2= new Version(3,2);

        @Override
        public String toString() {
            return major + "." + minor;
        }
    }

    private record IndexEntry(long offset, long length, byte[] prefix) {}

    private Path file = null;
    private SeekableByteChannel handle = null;

    private Map<Path, byte[]> files = new HashMap<>();
    private Map<Path, List<IndexEntry>> indexes = new HashMap<>();

    private Version version;
    private int padlength = 0;
    private long key;
    private boolean verbose = false;


    private static final String RPA2_MAGIC = "RPA-2.0 ";
    private static final String RPA3_MAGIC = "RPA-3.0 ";
    private static final String RPA3_2_MAGIC = "RPA-3.2 ";


    // For backward compatibility, otherwise Python3-packed archives won't be read by Python2
    private static final int PICKLE_PROTOCOL = 2;

    public RenPyArchive(Path file, Version version, Integer padlength, Long key, Boolean verbose) throws IOException
    {
        this.padlength = Objects.requireNonNullElse(padlength, 0);
        this.key = Objects.requireNonNullElse(key, Long.parseLong("DEADBEEF", 16));
        this.verbose = Objects.requireNonNullElse(verbose, false);

        if (file != null) {
            load(file);
        } else {
            this.version = Objects.requireNonNullElse(version, Version.V3);
        }
    }

    @Override
    public void close() throws IOException
    {
        if (handle != null) {
            handle.close();
        }
    }

    /**
     * Determine archive version.
     */
    private Version getVersion() throws IOException
    {
        handle.position(0);
        String magic = new BufferedReader(new InputStreamReader(Channels.newInputStream(handle), StandardCharsets.UTF_8)).readLine();

        if (magic.startsWith(RPA3_2_MAGIC)) {
            return Version.V3_2;
        }
        else if (magic.startsWith(RPA3_MAGIC)) {
            return Version.V3;
        }
        else if (magic.startsWith(RPA2_MAGIC)) {
            return Version.V2;
        }
        else if (file.getFileName().toString().endsWith(".rpi")) {
            return Version.V1;
        }

        throw new IllegalArgumentException("The given file is not a valid Ren'Py archive, or an unsupported version");
    }

    /**
     * Extract file indexes from opened archive.
     */
    private Map<Path, List<IndexEntry>> extractIndexes() throws IOException
    {
        handle.position(0);
        Map<Path, List<IndexEntry>> indexes = null;
        int offset = 0;
        if (List.of(Version.V2, Version.V3, Version.V3_2).contains(version))
        {
            // Fetch metadata.
            String metadata = new BufferedReader(new InputStreamReader(Channels.newInputStream(handle),
                                                                       StandardCharsets.UTF_8)).readLine();
            String[] vals = metadata.split("\\s");
            //int
            offset = Integer.parseInt(vals[1], 16);
            if (version.equals(Version.V3))
            {
                key = 0;
                for (int i = 2; i < vals.length; i++)
                {
                    key ^= Long.parseLong(vals[i], 16);
                }
            }
            else if (version.equals(Version.V3_2))
            {
                key = 0;
                for (int i = 3; i < vals.length; i++)
                {
                    key ^= Long.parseLong(vals[i], 16);
                }
            }

            handle.position(offset);
        }

        // Load in indexes.
        Map<?, ?> rawIndexes = (Map<?, ?>) new Unpickler().load(new InflaterInputStream(Channels.newInputStream(handle)));
        indexes = new HashMap<>(rawIndexes.size());
        for (Entry<?, ?> e : rawIndexes.entrySet()) {
            Path path = Path.of(e.getKey().toString());
            @SuppressWarnings("unchecked")
            List<Object[]> rawParts = (List<Object[]>) e.getValue();
            List<IndexEntry> parts = new ArrayList<>(rawParts.size());
            for (Object[] rawPart : rawParts) {
                long partOffset = ((Number) rawPart[0]).longValue();
                long partSize = ((Number) rawPart[1]).longValue();
                byte[] partPrefix = new byte[0];
                if (rawPart.length > 2) {
                    if (rawPart[2] instanceof byte[] prefix) {
                        partPrefix = prefix;
                    }
                    else {
                        partPrefix = rawPart[2].toString().getBytes(StandardCharsets.ISO_8859_1);
                    }
                }
                parts.add(new IndexEntry(partOffset, partSize, partPrefix));
            }
            indexes.put(path, parts);
        }

        // Deobfuscate indexes.
        if (List.of(Version.V3, Version.V3_2).contains(version))
        {
            for (List<IndexEntry> parts : indexes.values()) {
                for (int i = 0 ; i < parts.size() ; i++) {
                    IndexEntry part = parts.get(i);
                    part = new IndexEntry(part.offset ^ key, part.length ^ key, part.prefix);
                    parts.set(i, part);
                }
            }
        }

        return indexes;
    }


    /**
     * Generate pseudorandom padding (for whatever reason).
     */
    private byte[] generatePadding() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = random.nextInt(1, padlength);

        StringBuilder padding = new StringBuilder();
        while (length > 0) {
            padding.append(Character.toChars(random.nextInt(1, 255)));
            length--;
        }

        return padding.toString().getBytes(StandardCharsets.UTF_8);
    }


    /**
     * Converts a filename to archive format.
     */
    private String convertFilename(Path filename) {
        FileSystem fs = filename.getFileSystem();
        filename = filename.normalize();
        if (filename.isAbsolute()) {
            filename = filename.getRoot().relativize(filename);
        }
        return filename.toString().replace(fs.getSeparator(), "/");
    }

    /**
     * Debug (verbose) messages.
     */
    private void verbosePrint(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }

    /**
     * List files in archive and current internal storage.
     */
    public List<Path> list() {
        List<Path> list = new ArrayList<>(indexes.size() + files.size());
        list.addAll(indexes.keySet());
        list.addAll(files.keySet());
        return list;
    }

    /**
     * Check if a file exists in the archive.
     */
    public boolean hasFile(Path filename) {
        return indexes.containsKey(filename) || files.containsKey(filename);
    }

    /**
     * Read file from archive or internal storage.
     */
    public byte[] read(Path filename) throws IOException
    {
        // Check if the file exists in our indexes.
        if (!files.containsKey(filename) && !indexes.containsKey(filename)) {
            throw new NoSuchElementException("The requested file "+filename+" does not exist in the given Ren'Py archive");
        }

        // If it's in our opened archive index, and our archive handle isn't valid, something is obviously wrong.
        if (!files.containsKey(filename) && indexes.containsKey(filename) && handle == null) {
            throw new NoSuchElementException("The requested file "+filename+" does not exist in the given Ren'Py archive");
        }

        // Check our simplified internal indexes first, in case someone wants to read a file they added before without saving, for some unholy reason.
        if (files.containsKey(filename))
        {
            verbosePrint("Reading file "+filename+" from internal storage...");
            return files.get(filename);
        }
        // We need to read the file from our open archive.
        else
        {
            // Read offset and length, seek to the offset and read the file contents.
            IndexEntry part = indexes.get(filename).getFirst();

            verbosePrint("Reading file "+filename+" from data file "+file+"... (offset = "+part.offset+", length = "+part.length+" bytes)");
            handle.position(part.offset);
            byte[] content = new byte[(int) part.length];
            System.arraycopy(part.prefix, 0, content, 0, part.prefix.length);
            int sizeToRead = (int) (part.length - part.prefix.length);
            int read = Channels.newInputStream(handle).read(content, part.prefix.length, sizeToRead);
            if (read != sizeToRead) {
                content = Arrays.copyOf(content, read);
            }
            return content;
        }
    }
    

    /**
     * Modify a file in archive or internal storage.
     */
    public void change(Path filename, byte[] content)
    {
         // Our 'change' is basically removing the file from our indexes first, and then re-adding it.
        remove(filename);
        add(filename, content);
    }


    /**
     * Add a file to the internal storage.
     */
    public void add(Path filename, byte[] content) {
        if (hasFile(filename)) {
            throw new IllegalArgumentException("File "+filename+" already exists in archive");
        }

        verbosePrint("Adding file "+filename+" to archive... (length = "+content.length+" bytes)");
        files.put(filename, content);
    }


    /**
     * Remove a file from archive or internal storage.
     */
    public void remove(Path filename) {
        if (files.containsKey(filename))
        {
            verbosePrint("Removing file "+filename+" from internal storage...");
            files.remove(filename);
        }
        else if (indexes.containsKey(filename))
        {
            verbosePrint("Removing file "+filename+" from archive indexes...");
            indexes.remove(filename);
        }
        else
        {
            throw new NoSuchElementException("The requested file "+filename+" does not exist in this archive");
        }
    }

    /**
     * Load archive.
     */
    private void load(Path file) throws IOException
    {
        if (handle != null) {
            handle.close();
        }
        this.file = file;
        files = new HashMap<>();
        handle = Files.newByteChannel(file, StandardOpenOption.READ);
        version = getVersion();
        indexes = extractIndexes();
    }

    /**
     * Save current state into a new file, merging archive and internal storage, rebuilding indexes, and optionally saving in another format version.
     */
    public void save(Path filename) throws IOException
    {
        if (filename == null) {
            filename = this.file;
        }
        if (filename == null) {
            throw new IllegalArgumentException("No target file found for saving archive");
        }
        if (!List.of(Version.V2, Version.V3).contains(version)) {
            throw new IllegalStateException("Saving is only supported for version 2 and 3 archives");
        }

        verbosePrint("Rebuilding archive index...");
        // Fill our own files structure with the files added or changed in this session.
        Map<Path, byte[]> files = this.files;
        // First, read files from the current archive into our files structure.
        for (Path file : List.copyOf(this.indexes.keySet())) {
            byte[] content = read(file);
            // Remove from indexes array once read, add to our own array.
            this.indexes.remove(file);
            files.put(file, content);
        }

        // Predict header length, we'll write that one last.
        long offset = 0;
        if (this.version.equals(Version.V3))
            offset = 34;
        else if (this.version.equals(Version.V2))
            offset = 25;

        SeekableByteChannel archiveC = Files.newByteChannel(file, StandardOpenOption.WRITE);
        archiveC.position(offset);
        OutputStream archive = Channels.newOutputStream(archiveC);

        // Build our own indexes while writing files to the archive.
        Map<String, List<Object[]>> indexes = new HashMap<>();
        verbosePrint("Writing files to archive file...");
        for (Entry<Path, byte[]> e : files.entrySet())
        {
            Path file = e.getKey();
            byte[] content = e.getValue();
            // Generate random padding, for whatever reason.
            if (this.padlength > 0) {
                byte[] padding = generatePadding();
                archive.write(padding);
                offset += padding.length;
            }

            archive.write(content);
            // Update index.
            if (version.equals(Version.V3))
                indexes.put(convertFilename(file), List.<Object[]>of(new Object[]{offset ^ key, content.length ^ key}));
            else if (version.equals(Version.V2))
                indexes.put(convertFilename(file), List.<Object[]>of(new Object[]{offset, content.length}));
            offset += content.length;
        }

        // Write the indexes.
        verbosePrint("Writing archive index to archive file...");
        new Pickler().dump(indexes, new DeflaterOutputStream(archive));
        // Now write the header.
        verbosePrint("Writing header to archive file... (version = RPAv"+version+")");
        archiveC.position(0);
        archive = Channels.newOutputStream(archiveC);//Not sure if needed
        if (version.equals(Version.V3))
            archive.write(String.format("%s%016x %08x\n", RPA3_MAGIC, offset, key).getBytes(StandardCharsets.UTF_8));
        else
            archive.write(String.format("%s%016x\n", RPA3_MAGIC, offset).getBytes(StandardCharsets.UTF_8));
        // We're done, close it.
        archive.close();

        // Reload the file in our inner database.
        load(filename);
    }
//
//if __name__ == "__main__":
//    import argparse
//
//    parser = argparse.ArgumentParser(
//        description='A tool for working with Ren\'Py archive files.',
//        epilog='The FILE argument can optionally be in ARCHIVE=REAL format, mapping a file in the archive file system to a file on your real file system. An example of this: rpatool -x test.rpa script.rpyc=/home/foo/test.rpyc',
//        add_help=False)
//
//    parser.add_argument('archive', metavar='ARCHIVE', help='The Ren\'py archive file to operate on.')
//    parser.add_argument('files', metavar='FILE', nargs='*', action='append', help='Zero or more files to operate on.')
//
//    parser.add_argument('-l', '--list', action='store_true', help='List files in archive ARCHIVE.')
//    parser.add_argument('-x', '--extract', action='store_true', help='Extract FILEs from ARCHIVE.')
//    parser.add_argument('-c', '--create', action='store_true', help='Creative ARCHIVE from FILEs.')
//    parser.add_argument('-d', '--delete', action='store_true', help='Delete FILEs from ARCHIVE.')
//    parser.add_argument('-a', '--append', action='store_true', help='Append FILEs to ARCHIVE.')
//
//    parser.add_argument('-2', '--two', action='store_true', help='Use the RPAv2 format for creating/appending to archives.')
//    parser.add_argument('-3', '--three', action='store_true', help='Use the RPAv3 format for creating/appending to archives (default).')
//
//    parser.add_argument('-k', '--key', metavar='KEY', help='The obfuscation key used for creating RPAv3 archives, in hexadecimal (default: 0xDEADBEEF).')
//    parser.add_argument('-p', '--padding', metavar='COUNT', help='The maximum number of bytes of padding to add between files (default: 0).')
//    parser.add_argument('-o', '--outfile', help='An alternative output archive file when appending to or deleting from archives, or output directory when extracting.')
//
//    parser.add_argument('-h', '--help', action='help', help='Print this help and exit.')
//    parser.add_argument('-v', '--verbose', action='store_true', help='Be a bit more verbose while performing operations.')
//    parser.add_argument('-V', '--version', action='version', version='rpatool v0.8', help='Show version information.')
//    arguments = parser.parse_args()
//
//    # Determine RPA version.
//    if arguments.two:
//        version = 2
//    else:
//        version = 3
//
//    # Determine RPAv3 key.
//    if 'key' in arguments and arguments.key is not None:
//        key = int(arguments.key, 16)
//    else:
//        key = 0xDEADBEEF
//
//    # Determine padding bytes.
//    if 'padding' in arguments and arguments.padding is not None:
//        padding = int(arguments.padding)
//    else:
//        padding = 0
//
//    # Determine output file/directory and input archive
//    if arguments.create:
//        archive = None
//        output = _unicode(arguments.archive)
//    else:
//        archive = _unicode(arguments.archive)
//        if 'outfile' in arguments and arguments.outfile is not None:
//            output = _unicode(arguments.outfile)
//        else:
//            # Default output directory for extraction is the current directory.
//            if arguments.extract:
//                output = '.'
//            else:
//                output = _unicode(arguments.archive)
//
//    # Normalize files.
//    if len(arguments.files) > 0 and isinstance(arguments.files[0], list):
//        arguments.files = arguments.files[0]
//
//    try:
//        archive = RenPyArchive(archive, padlength=padding, key=key, version=version, verbose=arguments.verbose)
//    except IOError as e:
//        print('Could not open archive file {0} for reading: {1}'.format(archive, e), file=sys.stderr)
//        sys.exit(1)
//
//    if arguments.create or arguments.append:
//        # We need this seperate function to recursively process directories.
//        def add_file(filename):
//            # If the archive path differs from the actual file path, as given in the argument,
//            # extract the archive path and actual file path.
//            if filename.find('=') != -1:
//                (outfile, filename) = filename.split('=', 2)
//            else:
//                outfile = filename
//
//            if os.path.isdir(filename):
//                for file in os.listdir(filename):
//                    # We need to do this in order to maintain a possible ARCHIVE=REAL mapping between directories.
//                    add_file(outfile + os.sep + file + '=' + filename + os.sep + file)
//            else:
//                try:
//                    with open(filename, 'rb') as file:
//                        archive.add(outfile, file.read())
//                except Exception as e:
//                    print('Could not add file {0} to archive: {1}'.format(filename, e), file=sys.stderr)
//
//        # Iterate over the given files to add to archive.
//        for filename in arguments.files:
//            add_file(_unicode(filename))
//
//        # Set version for saving, and save.
//        archive.version = version
//        try:
//            archive.save(output)
//        except Exception as e:
//            print('Could not save archive file: {0}'.format(e), file=sys.stderr)
//    elif arguments.delete:
//        # Iterate over the given files to delete from the archive.
//        for filename in arguments.files:
//            try:
//                archive.remove(filename)
//            except Exception as e:
//                print('Could not delete file {0} from archive: {1}'.format(filename, e), file=sys.stderr)
//
//        # Set version for saving, and save.
//        archive.version = version
//        try:
//            archive.save(output)
//        except Exception as e:
//            print('Could not save archive file: {0}'.format(e), file=sys.stderr)
//    elif arguments.extract:
//        # Either extract the given files, or all files if no files are given.
//        if len(arguments.files) > 0:
//            files = arguments.files
//        else:
//            files = archive.list()
//
//        # Create output directory if not present.
//        if not os.path.exists(output):
//            os.makedirs(output)
//
//        # Iterate over files to extract.
//        for filename in files:
//            if filename.find('=') != -1:
//                (outfile, filename) = filename.split('=', 2)
//            else:
//                outfile = filename
//
//            try:
//                contents = archive.read(filename)
//
//                # Create output directory for file if not present.
//                if not os.path.exists(os.path.dirname(os.path.join(output, outfile))):
//                    os.makedirs(os.path.dirname(os.path.join(output, outfile)))
//
//                with open(os.path.join(output, outfile), 'wb') as file:
//                    file.write(contents)
//            except Exception as e:
//                print('Could not extract file {0} from archive: {1}'.format(filename, e), file=sys.stderr)
//    elif arguments.list:
//        # Print the sorted file list.
//        list = archive.list()
//        list.sort()
//        for file in list:
//            print(file)
//    else:
//        print('No operation given :(')
//        print('Use {0} --help for usage details.'.format(sys.argv[0]))

}

