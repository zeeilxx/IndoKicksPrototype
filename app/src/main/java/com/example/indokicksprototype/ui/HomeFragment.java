//package com.example.indokicksprototype.ui.homepage;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.ProgressBar;
//import android.widget.Spinner;
//import android.widget.TextView;
//
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.indokicksprototype.R;
//import com.example.indokicksprototype.model.StandingRow;
//import com.example.indokicksprototype.model.Team;
//import com.example.indokicksprototype.network.ApiClient;
//import com.example.indokicksprototype.network.ApiClientBackend;
//import com.example.indokicksprototype.network.StandingsApiResponse;
//import com.example.indokicksprototype.network.TeamsApiResponse;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class HomeFragment extends Fragment {
//
//    private static final int LIGA_1_ID = 274;
//
//    private Spinner spinnerSeasonHome;
//    private ProgressBar progressStandings;
//    private TextView tvErrorStandings;
//    private StandingPreviewAdapter standingsPreviewAdapter;
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_homepage, container, false);
//    }
//
//    @Override
//    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        spinnerSeasonHome = view.findViewById(R.id.spinnerSeasonHome);
//        progressStandings = view.findViewById(R.id.progressStandingsHome);
//        tvErrorStandings = view.findViewById(R.id.tvErrorStandingsHome);
//
//        RecyclerView rv = view.findViewById(R.id.rvStandingsPreview);
//        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
//        standingsPreviewAdapter = new StandingPreviewAdapter();
//        rv.setAdapter(standingsPreviewAdapter);
//
//        setupSeasonSpinner();
//
//        // default load season terakhir (2025)
//        spinnerSeasonHome.setSelection(4); // [2021,2022,2023,2024,2025] -> index 4
//        loadStandingsPreview(getSelectedSeason());
//    }
//
//    private void setupSeasonSpinner() {
//        List<Integer> seasons = new ArrayList<>();
//        seasons.add(2021);
//        seasons.add(2022);
//        seasons.add(2023);
//        seasons.add(2024);
//        seasons.add(2025);
//
//        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
//                requireContext(),
//                android.R.layout.simple_spinner_dropdown_item,
//                seasons
//        );
//        spinnerSeasonHome.setAdapter(adapter);
//
//        spinnerSeasonHome.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
//                loadStandingsPreview(getSelectedSeason());
//            }
//
//            @Override
//            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
//        });
//    }
//
//    private int getSelectedSeason() {
//        Object o = spinnerSeasonHome.getSelectedItem();
//        if (o instanceof Integer) return (Integer) o;
//        return 2025;
//    }
//
//    private void loadStandingsPreview(int season) {
//        showLoading(true);
//        tvErrorStandings.setVisibility(View.GONE);
//
//        boolean useBackend = (season == 2024 || season == 2025);
//
//        Call<StandingsApiResponse> call = useBackend
//                ? ApiClientBackend.getService().getStandings(LIGA_1_ID, season)
//                : ApiClient.getService().getStandings(LIGA_1_ID, season);
//
//        call.enqueue(new Callback<StandingsApiResponse>() {
//            @Override
//            public void onResponse(Call<StandingsApiResponse> call, Response<StandingsApiResponse> response) {
//                if (!isAdded()) return;
//                showLoading(false);
//
//                if (!response.isSuccessful() || response.body() == null || response.body().getResponse() == null) {
//                    showError("Gagal memuat standings.");
//                    standingsPreviewAdapter.setItems(null);
//                    return;
//                }
//
//                List<StandingsApiResponse.ResponseItem> raw = response.body().getResponse();
//                List<StandingRow> rows = mapStandings(raw);
//
//                if (rows == null || rows.isEmpty()) {
//                    showError("Data klasemen kosong.");
//                    standingsPreviewAdapter.setItems(null);
//                    return;
//                }
//
//                // ambil top 5
//                List<StandingRow> top5 = rows.size() > 5 ? rows.subList(0, 5) : rows;
//
//                // kalau season 2024/2025: overlay logo dari API luar (teams season 2023)
//                if (useBackend) {
//                    overlayLogosFromExternal(top5);
//                } else {
//                    standingsPreviewAdapter.setItems(top5);
//                }
//            }
//
//            @Override
//            public void onFailure(Call<StandingsApiResponse> call, Throwable t) {
//                if (!isAdded()) return;
//                showLoading(false);
//                showError("Error jaringan: " + t.getMessage());
//                standingsPreviewAdapter.setItems(null);
//            }
//        });
//    }
//
//    private void overlayLogosFromExternal(List<StandingRow> topRows) {
//        // Ambil teams dari API luar (season 2023) untuk logo (sesuai aturan kamu)
//        ApiClient.getService().getTeams(LIGA_1_ID, 2023).enqueue(new Callback<TeamsApiResponse>() {
//            @Override
//            public void onResponse(Call<TeamsApiResponse> call, Response<TeamsApiResponse> response) {
//                if (!isAdded()) return;
//
//                Map<Integer, String> logoById = new HashMap<>();
//                Map<String, String> logoByName = new HashMap<>();
//
//                if (response.isSuccessful() && response.body() != null && response.body().getResponse() != null) {
//                    for (TeamsApiResponse.ResponseItem r : response.body().getResponse()) {
//                        if (r == null || r.team == null) continue;
//                        logoById.put(r.team.id, r.team.logo);
//                        if (r.team.name != null) {
//                            logoByName.put(r.team.name.trim().toLowerCase(), r.team.logo);
//                        }
//                    }
//                }
//
//                // apply overlay
//                for (StandingRow row : topRows) {
//                    Team t = row.getTeam();
//                    if (t == null) continue;
//
//                    String logo = null;
//
//                    // coba by id (kalau model Team kamu punya id, kalau tidak, akan dilewati)
//                    // Model Team kamu (com.example.indokicksprototype.model.Team) biasanya punya getId().
//                    try {
//                        int id = t.getId();
//                        logo = logoById.get(id);
//                    } catch (Exception ignored) { }
//
//                    // fallback by name
//                    if (logo == null && t.getName() != null) {
//                        logo = logoByName.get(t.getName().trim().toLowerCase());
//                    }
//
//                    if (logo != null) {
//                        t.setLogo(logo);
//                    }
//                }
//
//                standingsPreviewAdapter.setItems(topRows);
//            }
//
//            @Override
//            public void onFailure(Call<TeamsApiResponse> call, Throwable t) {
//                if (!isAdded()) return;
//                // tetap tampilkan tanpa overlay
//                standingsPreviewAdapter.setItems(topRows);
//            }
//        });
//    }
//
//    private List<StandingRow> mapStandings(List<StandingsApiResponse.ResponseItem> raw) {
//        List<StandingRow> out = new ArrayList<>();
//        if (raw == null || raw.isEmpty()) return out;
//
//        StandingsApiResponse.ResponseItem item0 = raw.get(0);
//        if (item0 == null || item0.league == null || item0.league.standings == null || item0.league.standings.isEmpty())
//            return out;
//
//        List<StandingsApiResponse.Standing> list = item0.league.standings.get(0);
//        if (list == null) return out;
//
//        for (StandingsApiResponse.Standing s : list) {
//            if (s == null || s.team == null || s.all == null || s.all.goals == null) continue;
//
//            Team team = new Team(s.team.id, s.team.name, s.team.logo);
//
//            StandingRow row = new StandingRow(
//                    s.rank,
//                    team,
//                    s.all.played,
//                    s.all.win,
//                    s.all.draw,
//                    s.all.lose,
//                    s.all.goals.goalsFor,
//                    s.all.goals.goalsAgainst,
//                    s.goalsDiff,
//                    s.points,
//                    s.form
//            );
//
//            out.add(row);
//        }
//
//        return out;
//    }
//
//    private void showLoading(boolean loading) {
//        progressStandings.setVisibility(loading ? View.VISIBLE : View.GONE);
//    }
//
//    private void showError(String msg) {
//        tvErrorStandings.setVisibility(View.VISIBLE);
//        tvErrorStandings.setText(msg);
//    }
//}
