package com.example.franchisesapi.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FranchiseTest {

    @Test
    void builderAppliesTheDefaultValues() {
        Franchise franchise = Franchise.builder().name("Burger").build();
        Branch branch = Branch.builder().id("b1").name("Centro").build();
        Product product = Product.builder().id("p1").name("Papas").build();

        assertThat(franchise.getStatus()).isEqualTo(FranchiseStatus.ACTIVE);
        assertThat(franchise.isActive()).isTrue();
        assertThat(branch.getStatus()).isEqualTo(BranchStatus.OPEN);
        assertThat(product.getCurrency()).isEqualTo("COP");
        assertThat(product.isActive()).isTrue();
    }

    @Test
    void builderStartsWithEmptyMutableLists() {
        Franchise franchise = Franchise.builder().name("Burger").build();
        franchise.getBranches().add(Branch.builder().id("b1").name("Centro").build());

        assertThat(franchise.getBranches()).hasSize(1);
        assertThat(franchise.getBranches().getFirst().getProducts()).isEmpty();
    }

    @Test
    void findBranchReturnsTheBranchWithTheGivenId() {
        Branch center = Branch.builder().id("b1").name("Centro").build();
        Franchise franchise = Franchise.builder().name("Burger").branches(List.of(center)).build();

        assertThat(franchise.findBranch("b1")).contains(center);
        assertThat(franchise.findBranch("missing")).isEmpty();
    }

    @Test
    void branchNameAndCodeChecksIgnoreCase() {
        Franchise franchise = Franchise.builder()
                .name("Burger")
                .branches(List.of(Branch.builder().id("b1").code("BOG-001").name("Centro").build()))
                .build();

        assertThat(franchise.hasBranchNamed("centro")).isTrue();
        assertThat(franchise.hasBranchNamed("Norte")).isFalse();
        assertThat(franchise.hasBranchWithCode("bog-001")).isTrue();
        assertThat(franchise.hasBranchWithCode("MED-001")).isFalse();
        assertThat(franchise.hasBranchWithCode(null)).isFalse();
    }

    @Test
    void otherBranchNamedSkipsTheBranchBeingRenamed() {
        Franchise franchise = Franchise.builder()
                .name("Burger")
                .branches(List.of(
                        Branch.builder().id("b1").name("Centro").build(),
                        Branch.builder().id("b2").name("Norte").build()))
                .build();

        assertThat(franchise.hasOtherBranchNamed("b1", "CENTRO")).as("its own name does not count").isFalse();
        assertThat(franchise.hasOtherBranchNamed("b1", "norte")).isTrue();
        assertThat(franchise.hasOtherBranchNamed("b1", "Sur")).isFalse();
    }

    @Test
    void productNameAndSkuChecksIgnoreCase() {
        Branch branch = Branch.builder().id("b1").name("Centro")
                .products(List.of(Product.builder().id("p1").sku("HAM-001").name("Hamburguesa").build()))
                .build();

        assertThat(branch.hasProductNamed("HAMBURGUESA")).isTrue();
        assertThat(branch.hasProductWithSku("ham-001")).isTrue();
        assertThat(branch.hasProductWithSku("PAP-001")).isFalse();
        assertThat(branch.hasProductWithSku(null)).isFalse();
    }

    @Test
    void removeProductDeletesOnlyTheMatchingProduct() {
        Product fries = Product.builder().id("p1").name("Papas").stock(10).build();
        Product soda = Product.builder().id("p2").name("Gaseosa").stock(5).build();
        Branch branch = Branch.builder().id("b1").name("Centro")
                .products(new ArrayList<>(List.of(fries, soda)))
                .build();

        assertThat(branch.removeProduct("p1")).isTrue();
        assertThat(branch.removeProduct("p1")).isFalse();
        assertThat(branch.getProducts()).containsExactly(soda);
    }

    @Test
    void branchIsClosedOnlyWhenItsStatusIsClosed() {
        assertThat(Branch.builder().status(BranchStatus.CLOSED).build().isClosed()).isTrue();
        assertThat(Branch.builder().status(BranchStatus.TEMPORARILY_CLOSED).build().isClosed()).isFalse();
        assertThat(Branch.builder().build().isClosed()).isFalse();
    }

    @Test
    void findTopStockProductReturnsTheProductWithMostStock() {
        Product fries = Product.builder().id("p1").name("Papas").stock(10).build();
        Product soda = Product.builder().id("p2").name("Gaseosa").stock(30).build();
        Product burger = Product.builder().id("p3").name("Hamburguesa").stock(20).build();
        Branch branch = Branch.builder().id("b1").name("Centro").products(List.of(fries, soda, burger)).build();

        assertThat(branch.findTopStockProduct()).contains(soda);
    }

    @Test
    void findTopStockProductKeepsTheFirstProductOnATie() {
        Product fries = Product.builder().id("p1").name("Papas").stock(30).build();
        Product soda = Product.builder().id("p2").name("Gaseosa").stock(30).build();
        Branch branch = Branch.builder().id("b1").name("Centro").products(List.of(fries, soda)).build();

        assertThat(branch.findTopStockProduct()).contains(fries);
    }

    @Test
    void findTopStockProductIsEmptyForABranchWithoutProducts() {
        assertThat(Branch.builder().id("b1").name("Centro").build().findTopStockProduct()).isEmpty();
    }

    @Test
    void productIsLowStockWhenStockReachesTheMinimum() {
        assertThat(Product.builder().stock(5).minimumStock(10).build().isLowStock()).isTrue();
        assertThat(Product.builder().stock(10).minimumStock(10).build().isLowStock()).isTrue();
        assertThat(Product.builder().stock(11).minimumStock(10).build().isLowStock()).isFalse();
        assertThat(Product.builder().stock(0).build().isLowStock()).isTrue();
    }

}
