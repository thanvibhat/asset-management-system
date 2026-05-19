import { Component, EventEmitter, Input, OnInit, Output, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil, of } from 'rxjs';
import { MasterService } from '../../../services/master.service';
import { Asset, AssetCategory } from '../../../models/models';

@Component({
  selector: 'app-asset-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './asset-form.component.html',
  styleUrls: ['./asset-form.component.css']
})
export class AssetFormComponent implements OnInit, OnDestroy {
  private _asset: any = null;
  @Input() set asset(val: any) {
    this._asset = val;
    this.checkAndInitDynamicFields();
  }
  get asset() { return this._asset; }

  @Input() editMode = false;
  
  private _products: any[] = [];
  @Input() set products(val: any[]) {
    this._products = val;
    this.checkAndInitDynamicFields();
  }
  get products() { return this._products; }

  @Input() categories: AssetCategory[] = [];
  @Input() locations: string[] = [];
  @Input() manufacturers: string[] = [];
  @Input() vendors: any[] = [];
  
  @Output() save = new EventEmitter<any>();
  @Output() cancel = new EventEmitter<void>();

  assetForm: FormGroup;
  dynamicFields: any[] = [];
  loadingNextTag = false;
  
  // Autocomplete state
  showManufacturerDropdown = false;
  manufacturerSuggestions: string[] = [];
  manufacturerSearch$ = new Subject<string>();
  showManufacturerInput = false;
  
  showLocationDropdown = false;
  locationSuggestions: string[] = [];

  showProductDropdown = false;
  filteredProducts: any[] = [];
  productSearch = '';

  showCategoryDropdown = false;
  filteredCategories: any[] = [];
  categorySearch = '';

  showParentDropdown = false;
  filteredParents: any[] = [];
  parentSearch = '';
  allAssets: any[] = [];

  private destroy$ = new Subject<void>();

  statuses = ['AVAILABLE', 'ALLOCATED', 'UNDER_MAINTENANCE', 'DAMAGED', 'RETIRED', 'LOST', 'DISPOSED'];

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private masterService: MasterService
  ) {
    this.assetForm = this.fb.group({
      id: [null],
      productId: [null, Validators.required],
      assetTag: [{ value: '', disabled: true }],
      name: ['', Validators.required],
      description: [''],
      categoryId: [null, Validators.required],
      status: ['AVAILABLE', Validators.required],
      purchaseDate: ['', Validators.required],
      purchaseCost: [null],
      location: ['', Validators.required],
      serialNumber: [''],
      manufacturer: [''],
      model: [''],
      vendorId: [null, Validators.required],
      warrantyMonths: [null],
      parentId: [null],
      depreciationRate: [0],
      disposalDate: [null],
      dynamicAttributes: this.fb.group({})
    });
  }

  ngOnInit(): void {
    if (this.asset) {
      // In edit mode, show manufacturer input if it has a value
      if (this.asset.manufacturer) {
        this.showManufacturerInput = true;
      }
      
      // Set initial product and category search text
      if (this.asset.productId) {
        const product = this.products.find(p => p.id === this.asset.productId);
        if (product) {
          this.productSearch = product.productName;
        }
      }

      if (this.asset.categoryId) {
        const category = this.categories.find(c => c.id === this.asset.categoryId);
        if (category) {
          this.categorySearch = category.name;
        }
      }

      if (this.asset.parentId) {
        this.parentSearch = this.asset.parentTag || 'Linked Device';
      }

      this.assetForm.patchValue(this.asset);
    }

    // Setup manufacturer autocomplete
    this.manufacturerSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) return of([]);
        return this.masterService.searchManufacturers(query);
      }),
      takeUntil(this.destroy$)
    ).subscribe(results => {
      this.manufacturerSuggestions = results.map((m: any) => m.name);
      this.showManufacturerDropdown = this.manufacturerSuggestions.length > 0;
    });

    // Fetch all assets for parent linking
    this.http.get<any>('/api/assets?size=1000').subscribe({
      next: (data) => {
        this.allAssets = data.content || [];
        // If we have parentId, find its tag
        if (this.asset?.parentId) {
          const parent = this.allAssets.find(a => a.id === this.asset.parentId);
          if (parent) {
            this.parentSearch = `${parent.assetTag} - ${parent.name}`;
          }
        }
      }
    });
  }

  private checkAndInitDynamicFields(): void {
    if (this.asset && this.products && this.products.length > 0) {
      this.initDynamicAttributes(this.asset);
      // Re-patch to fill the newly created dynamic controls
      if (this.asset.dynamicAttributes) {
        const group = this.assetForm.get('dynamicAttributes') as FormGroup;
        group.patchValue(this.asset.dynamicAttributes);
      }
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onProductChange(): void {
    const productId = this.assetForm.get('productId')?.value;
    const product = this.products.find(p => p.id === +productId);
    
    if (product) {
      this.assetForm.patchValue({
        categoryId: product.categoryId,
        name: product.productName
      });

      if (product.categoryId) {
        const category = this.categories.find(c => c.id === product.categoryId);
        if (category) {
          this.categorySearch = category.name;
        }
      }

      if (product.manufacturer) {
        this.showManufacturerInput = true;
      }

      // Rebuild dynamic attributes based on product
      this.buildDynamicFieldsFromProduct(product);

      // Fetch next tag
      if (!this.editMode) {
        this.loadingNextTag = true;
        this.http.get(`/api/products/${product.id}/next-tag`, { responseType: 'text' }).subscribe({
          next: tag => {
            this.assetForm.get('assetTag')?.setValue(tag);
            this.loadingNextTag = false;
          },
          error: () => { this.loadingNextTag = false; }
        });
      }
    }
  }

  private initDynamicAttributes(asset: any): void {
    let fields: any[] = [];
    if (asset.productId) {
      const product = this.products.find(p => p.id === asset.productId);
      if (product) {
        fields = this.parseAttributes(product.additionalAttributes);
      }
    }
    
    this.dynamicFields = fields;
    this.updateDynamicGroup(fields, asset.dynamicAttributes);
  }

  private buildDynamicFieldsFromProduct(product: any): void {
    const fields = this.parseAttributes(product.additionalAttributes);
    this.dynamicFields = fields;
    this.updateDynamicGroup(fields);
  }

  private parseAttributes(attr: any): any[] {
    if (!attr) return [];
    try {
      const parsed = typeof attr === 'string' ? JSON.parse(attr) : attr;
      return (parsed || []).map((a: any) => ({
        name: a.name,
        type: (a.dataType || a.type).toLowerCase(),
        required: a.mandatory || a.required
      }));
    } catch (e) {
      console.error('Error parsing attributes', e);
      return [];
    }
  }

  private updateDynamicGroup(fields: any[], existingValues: any = {}): void {
    const group = this.assetForm.get('dynamicAttributes') as FormGroup;
    
    // Get current values to persist them if fields match
    const currentValues = { ...group.value, ...existingValues };
    
    // Clear current controls
    Object.keys(group.controls).forEach(key => group.removeControl(key));
    
    // Add new controls
    fields.forEach(field => {
      const value = currentValues[field.name] !== undefined ? currentValues[field.name] : (field.type === 'number' ? null : '');
      group.addControl(field.name, this.fb.control(value, field.required ? Validators.required : null));
    });
  }

  // Autocomplete helper methods
  toggleManufacturerInput(): void {
    this.showManufacturerInput = !this.showManufacturerInput;
    if (!this.showManufacturerInput) {
      this.assetForm.get('manufacturer')?.setValue('');
    }
  }

  searchManufacturer(event: any): void {
    const query = event.target.value;
    this.manufacturerSearch$.next(query);
  }

  selectManufacturer(name: string): void {
    this.assetForm.get('manufacturer')?.setValue(name);
    this.showManufacturerDropdown = false;
  }

  filterLocations(event: any): void {
    const query = event.target.value;
    if (!query) {
      this.locationSuggestions = [];
      this.showLocationDropdown = false;
      return;
    }
    this.locationSuggestions = this.locations
      .filter(l => l.toLowerCase().includes(query.toLowerCase()))
      .slice(0, 8);
    this.showLocationDropdown = this.locationSuggestions.length > 0;
  }

  selectLocation(name: string): void {
    this.assetForm.get('location')?.setValue(name);
    this.showLocationDropdown = false;
  }

  filterProducts(event: any): void {
    const query = event.target.value;
    this.productSearch = query;
    if (!query) {
      this.filteredProducts = [];
      this.showProductDropdown = false;
      this.assetForm.get('productId')?.setValue(null);
      return;
    }
    this.filteredProducts = this.products
      .filter(p => p.productName.toLowerCase().includes(query.toLowerCase()))
      .slice(0, 10);
    this.showProductDropdown = this.filteredProducts.length > 0;
  }

  selectProduct(product: any): void {
    this.productSearch = product.productName;
    this.assetForm.get('productId')?.setValue(product.id);
    this.showProductDropdown = false;
    this.onProductChange();
  }

  filterCategories(event: any): void {
    const query = event.target.value;
    this.categorySearch = query;
    if (!query) {
      this.filteredCategories = [];
      this.showCategoryDropdown = false;
      this.assetForm.get('categoryId')?.setValue(null);
      return;
    }
    this.filteredCategories = this.categories
      .filter(c => c.name.toLowerCase().includes(query.toLowerCase()))
      .slice(0, 10);
    this.showCategoryDropdown = this.filteredCategories.length > 0;
  }

  selectCategory(category: any): void {
    this.categorySearch = category.name;
    this.assetForm.get('categoryId')?.setValue(category.id);
    this.showCategoryDropdown = false;
  }

  filterParents(event: any): void {
    const query = event.target.value;
    this.parentSearch = query;
    if (!query) {
      this.filteredParents = [];
      this.showParentDropdown = false;
      this.assetForm.get('parentId')?.setValue(null);
      return;
    }
    this.filteredParents = this.allAssets
      .filter(a => (a.assetTag.toLowerCase().includes(query.toLowerCase()) || a.name.toLowerCase().includes(query.toLowerCase())) && a.id !== this.assetForm.get('id')?.value)
      .slice(0, 10);
    this.showParentDropdown = this.filteredParents.length > 0;
  }

  selectParent(asset: any): void {
    this.parentSearch = `${asset.assetTag} - ${asset.name}`;
    this.assetForm.get('parentId')?.setValue(asset.id);
    this.showParentDropdown = false;
  }

  submit(): void {
    if (this.assetForm.invalid) {
      this.assetForm.markAllAsTouched();
      return;
    }
    
    const formValue = this.assetForm.getRawValue();
    // Stringify dynamicAttributes if needed for backend
    const payload = {
      ...formValue,
      dynamicAttributes: JSON.stringify(formValue.dynamicAttributes)
    };
    
    this.save.emit(payload);
  }

  get dynamicGroup(): FormGroup {
    return this.assetForm.get('dynamicAttributes') as FormGroup;
  }

  formatStatus(status: string): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').toLowerCase().split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
  }
}
