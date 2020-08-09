package fi.dy.masa.malilib.gui.widget.list;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.icon.DefaultFileBrowserIconProvider;
import fi.dy.masa.malilib.gui.icon.FileBrowserIconProvider;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.DirectoryNavigationWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.entry.DirectoryEntryWidget;
import fi.dy.masa.malilib.gui.widget.util.DirectoryCache;
import fi.dy.masa.malilib.gui.widget.util.DirectoryNavigator;
import fi.dy.masa.malilib.util.DirectoryCreator;
import fi.dy.masa.malilib.util.FileUtils;

public class BaseFileBrowserWidget extends DataListWidget<DirectoryEntry> implements DirectoryNavigator
{
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final FileFilter ALWAYS_FALSE_FILE_FILTER = (file) -> false;
    public static final FileFilter ALWAYS_TRUE_FILE_FILTER = File::isFile;

    protected final FileBrowserIconProvider iconProvider = new DefaultFileBrowserIconProvider();
    protected final DirectoryNavigationWidget navigationWidget;
    protected final File rootDirectory;
    @Nullable protected final DirectoryCache cache;
    protected FileFilter fileFilter;
    protected String browserContext;
    protected File currentDirectory;

    public BaseFileBrowserWidget(int x, int y, int width, int height, File defaultDirectory, File rootDirectory,
                                 @Nullable DirectoryCache cache, @Nullable String browserContext)
    {
        super(x, y, width, height, Collections::emptyList);

        this.rootDirectory = rootDirectory;
        this.cache = cache;
        this.browserContext = browserContext != null ? browserContext : "";
        this.currentDirectory = cache != null ? cache.getCurrentDirectoryForContext(this.browserContext) : null;
        this.allowKeyboardNavigation = true;
        this.shouldSortList = true;
        this.entryWidgetFixedHeight = 14;

        if (this.currentDirectory == null)
        {
            this.currentDirectory = defaultDirectory;
        }

        this.navigationWidget = new DirectoryNavigationWidget(this.getX() + 2, this.getY() + 3, width, 14,
                                                              this.currentDirectory, rootDirectory, this, this.getIconProvider(), this.getRootDirectoryDisplayName());
        this.searchBarWidget = this.navigationWidget;

        this.setEntryWidgetFactory((wx, wy, ww, wh, li, oi, entry, lw) ->
                                    new DirectoryEntryWidget(wx, wy, ww, wh, li, oi, entry, this, this.iconProvider));

        this.setBackgroundColor(0xB0000000);
        this.setBorderColor(BaseScreen.COLOR_HORIZONTAL_BAR);
        this.setBackgroundEnabled(true);
        this.listPosition.setRightPadding(3);
    }

    public FileBrowserIconProvider getIconProvider()
    {
        return this.iconProvider;
    }

    @Override
    public void refreshEntries()
    {
        this.filteredContents.clear();

        File dir = this.currentDirectory;

        if (dir.isDirectory() && dir.canRead())
        {
            if (this.hasFilter())
            {
                this.addFilteredContents(dir);
            }
            else
            {
                this.addNonFilteredContents(dir);
            }
        }

        this.reCreateListEntryWidgets();
    }

    public void setFileFilter(FileFilter filter)
    {
        this.fileFilter = filter;
    }

    @Override
    protected Comparator<DirectoryEntry> getComparator()
    {
        return Comparator.naturalOrder();
    }

    @Nullable
    protected String getRootDirectoryDisplayName()
    {
        return null;
    }

    protected void updateDirectoryNavigationWidget()
    {
        this.navigationWidget.setCurrentDirectory(this.currentDirectory);
    }

    @Override
    public List<DirectoryEntry> getFilteredEntries()
    {
        return this.filteredContents;
    }

    protected void addNonFilteredContents(File dir)
    {
        List<DirectoryEntry> list = new ArrayList<>();

        // Show directories at the top
        this.addMatchingEntriesToList(this.getDirectoryFilter(), dir, list, null, null);
        Collections.sort(list);
        this.filteredContents.addAll(list);
        list.clear();

        this.addMatchingEntriesToList(this.getFileFilter(), dir, list, null, null);
        this.sortEntryList(list);
        this.filteredContents.addAll(list);
    }

    protected void addFilteredContents(File dir)
    {
        String filterText = this.getSearchBarWidget().getFilter().toLowerCase();
        List<DirectoryEntry> list = new ArrayList<>();
        this.addFilteredContents(dir, filterText, list, null);
        this.filteredContents.addAll(list);
    }

    protected void addFilteredContents(File dir, String filterText, List<DirectoryEntry> listOut, @Nullable String prefix)
    {
        List<DirectoryEntry> list = new ArrayList<>();
        this.addMatchingEntriesToList(this.getDirectoryFilter(), dir, list, filterText, prefix);
        Collections.sort(list);
        listOut.addAll(list);
        list.clear();

        for (File subDir : FileUtils.getSubDirectories(dir))
        {
            String pre;

            if (prefix != null)
            {
                pre = prefix + subDir.getName() + "/";
            }
            else
            {
                pre = subDir.getName() + "/";
            }

            this.addFilteredContents(subDir, filterText, list, pre);
            this.sortEntryList(list);
            listOut.addAll(list);
            list.clear();
        }

        this.addMatchingEntriesToList(this.getFileFilter(), dir, list, filterText, prefix);
        this.sortEntryList(list);
        listOut.addAll(list);
    }

    protected void addMatchingEntriesToList(FileFilter filter, File dir, List<DirectoryEntry> list, @Nullable String filterText, @Nullable String displayNamePrefix)
    {
        for (File file : dir.listFiles(filter))
        {
            DirectoryEntry entry = new DirectoryEntry(DirectoryEntryType.fromFile(file), dir, file.getName(), displayNamePrefix);

            if (filterText == null || this.matchesFilter(this.getSearchStringsForEntry(entry), filterText))
            {
                list.add(entry);
            }
        }
    }

    @Override
    protected List<String> getSearchStringsForEntry(DirectoryEntry entry)
    {
        return ImmutableList.of(FileUtils.getNameWithoutExtension(entry.getName().toLowerCase()));
    }

    protected File getRootDirectory()
    {
        return this.rootDirectory;
    }

    protected FileFilter getDirectoryFilter()
    {
        return FileUtils.DIRECTORY_FILTER;
    }

    protected FileFilter getFileFilter()
    {
        return this.fileFilter != null ? this.fileFilter : ALWAYS_FALSE_FILE_FILTER;
    }

    @Override
    protected int getHeightForListEntryWidget(int listIndex)
    {
        DirectoryEntry entry = this.filteredContents.get(listIndex);
        return entry.getType() == DirectoryEntryType.DIRECTORY ? 14 : this.entryWidgetFixedHeight;
    }

    protected boolean currentDirectoryIsRoot()
    {
        return this.currentDirectory.equals(this.getRootDirectory());
    }

    @Override
    public File getCurrentDirectory()
    {
        return this.currentDirectory;
    }

    @Override
    public void switchToDirectory(File dir)
    {
        this.clearSelection();

        this.currentDirectory = FileUtils.getCanonicalFileIfPossible(dir);

        if (this.cache != null)
        {
            this.cache.setCurrentDirectoryForContext(this.browserContext, dir);
        }

        this.refreshEntries();
        this.updateDirectoryNavigationWidget();
        this.resetScrollbarPosition();
    }

    @Override
    public void switchToRootDirectory()
    {
        this.switchToDirectory(this.getRootDirectory());
    }

    @Override
    public void switchToParentDirectory()
    {
        File parent = this.currentDirectory.getParentFile();

        if (this.currentDirectoryIsRoot() == false && parent != null &&
            this.currentDirectory.getAbsolutePath().contains(this.getRootDirectory().getAbsolutePath()))
        {
            this.switchToDirectory(parent);
        }
        else
        {
            this.switchToRootDirectory();
        }
    }

    @Override
    public boolean onKeyTyped(char typedChar, int keyCode)
    {
        if (super.onKeyTyped(typedChar, keyCode))
        {
            return true;
        }

        if ((keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_LEFT) && this.currentDirectoryIsRoot() == false)
        {
            this.switchToParentDirectory();
            return true;
        }
        else if ((keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_RETURN) &&
                         this.getLastSelectedEntry() != null && this.getLastSelectedEntry().getType() == DirectoryEntryType.DIRECTORY)
        {
            this.switchToDirectory(new File(this.getLastSelectedEntry().getDirectory(), this.getLastSelectedEntry().getName()));
            return true;
        }
        else if (keyCode == Keyboard.KEY_N && BaseScreen.isCtrlDown() && BaseScreen.isShiftDown())
        {
            DirectoryCreator creator = new DirectoryCreator(this.getCurrentDirectory(), this);
            TextInputScreen gui = new TextInputScreen("malilib.gui.title.create_directory", "", GuiUtils.getCurrentScreen(), creator);
            BaseScreen.openPopupGui(gui);
            return true;
        }

        return false;
    }

    protected void drawAdditionalContents(int mouseX, int mouseY)
    {
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, boolean hovered)
    {
        super.render(mouseX, mouseY, isActiveGui, hovered);

        this.drawAdditionalContents(mouseX, mouseY);
    }

    public static class DirectoryEntry implements Comparable<DirectoryEntry>
    {
        private final DirectoryEntryType type;
        private final File dir;
        private final String name;
        @Nullable private final String displaynamePrefix;

        public DirectoryEntry(DirectoryEntryType type, File dir, String name, @Nullable String displaynamePrefix)
        {
            this.type = type;
            this.dir = dir;
            this.name = name;
            this.displaynamePrefix = displaynamePrefix;
        }

        public DirectoryEntryType getType()
        {
            return this.type;
        }

        public File getDirectory()
        {
            return this.dir;
        }

        public String getName()
        {
            return this.name;
        }

        @Nullable
        public String getDisplayNamePrefix()
        {
            return this.displaynamePrefix;
        }

        public String getDisplayName()
        {
            return this.displaynamePrefix != null ? this.displaynamePrefix + this.name : this.name;
        }

        public File getFullPath()
        {
            return new File(this.dir, this.name);
        }

        @Override
        public int compareTo(DirectoryEntry other)
        {
            return this.name.toLowerCase(Locale.US).compareTo(other.getName().toLowerCase(Locale.US));
        }
    }

    public enum DirectoryEntryType
    {
        INVALID,
        DIRECTORY,
        FILE;

        public static DirectoryEntryType fromFile(File file)
        {
            if (file.exists() == false)
            {
                return INVALID;
            }
            else if (file.isDirectory())
            {
                return DIRECTORY;
            }
            else
            {
                return FILE;
            }
        }
    }
}
