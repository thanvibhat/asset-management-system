import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MasterService } from '../../services/master.service';
import { AssetService } from '../../services/asset.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-product-master',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-master.component.html',
  styleUrls: ['./product-master.component.css']
})
export class ProductMasterComponent implements OnInit {
  products: any[] = [];
  categories: any[] = [];
  loading = false;
  saving = false;
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 10;
  error = '';
  showProductModal = false;
  editProductId: number | null = null;
  productForm: any = {
    productName: '',
    categoryId: null,
    manufacturer: '',
    description: '',
    assetPrefix: '',
    depreciationPercentage: 0,
    attributeRows: []
  };

  constructor(
    private masterService: MasterService,
    private assetService: AssetService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.loadCategories();
  }

  loadProducts(): void {
    this.loading = true;
    this.masterService.getProducts(this.currentPage, this.pageSize).subscribe({
      next: p => {
        this.products = p.content;
        this.totalElements = p.totalElements;
        this.totalPages = p.totalPages;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadProducts();
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  loadCategories(): void {
    this.assetService.getCategories().subscribe(cats => {
      this.categories = cats;
    });
  }

  openProductModal(p?: any): void {
    if (p) {
      this.editProductId = p.id;
      this.productForm = {
        productName: p.productName,
        categoryId: p.categoryId,
        manufacturer: p.manufacturer,
        description: p.description,
        assetPrefix: p.assetPrefix,
        depreciationPercentage: p.depreciationPercentage || 0,
        attributeRows: this.parseAttributes(p.additionalAttributes)
      };
    } else {
      this.editProductId = null;
      this.productForm = {
        productName: '',
        categoryId: null,
        manufacturer: '',
        description: '',
        assetPrefix: '',
        depreciationPercentage: 0,
        attributeRows: []
      };
    }
    this.showProductModal = true;
    this.error = '';
  }

  addAttributeRow(): void {
    this.productForm.attributeRows.push({ name: '', dataType: 'String', mandatory: false });
  }

  removeAttributeRow(index: number): void {
    this.productForm.attributeRows.splice(index, 1);
  }

  saveProduct(): void {
    this.saving = true;
    const payload = {
      ...this.productForm,
      additionalAttributes: JSON.stringify(this.productForm.attributeRows)
    };
    delete payload.attributeRows;

    const obs = this.editProductId
      ? this.masterService.updateProduct(this.editProductId, payload)
      : this.masterService.createProduct(payload);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showProductModal = false;
        this.loadProducts();
      },
      error: err => {
        this.error = err.error?.message || 'Error saving product';
        this.saving = false;
      }
    });
  }

  deleteProduct(id: number): void {
    if (confirm('Are you sure you want to delete this product?')) {
      this.masterService.deleteProduct(id).subscribe(() => this.loadProducts());
    }
  }

  parseAttributes(json: string): any[] {
    try {
      return JSON.parse(json) || [];
    } catch {
      return [];
    }
  }
}
