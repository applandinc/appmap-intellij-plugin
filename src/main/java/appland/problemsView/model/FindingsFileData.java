package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class FindingsFileData {
    @SerializedName("findings")
    public List<ScannerFinding> findings;
}
