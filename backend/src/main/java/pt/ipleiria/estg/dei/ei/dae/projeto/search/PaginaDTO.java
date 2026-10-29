package pt.ipleiria.estg.dei.ei.dae.projeto.search;

import java.util.List;

public class PaginaDTO<T> {
    private final List<T> items;
    private final long total;
    private final int page;
    private final int size;

    public PaginaDTO(List<T> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<T> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }

    public int getTotalPages() {
        return size <= 0 ? 0 : (int) ((total + size - 1) / size);
    }
}
