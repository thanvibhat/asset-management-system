import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BusinessService } from '../../services/business.service';
import { Vendor } from '../../models/business.model';

@Component({
  selector: 'app-vendors',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vendors.component.html',
  styleUrls: ['./vendors.component.css']
})
export class VendorsComponent implements OnInit {
  vendors: Vendor[] = [];
  loading = true;
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 10;
  
  showModal = false;
  editMode = false;
  saving = false;
  error = '';
  deleteConfirmId: number | null = null;

  form: any = this.emptyForm();
  vendorTypes = ['PROCUREMENT', 'MAINTENANCE', 'BOTH'];

  constructor(private businessService: BusinessService) {}

  ngOnInit(): void {
    this.loadVendors();
  }

  loadVendors(): void {
    this.loading = true;
    this.businessService.getVendors(this.currentPage, this.pageSize).subscribe({
      next: p => {
        this.vendors = p.content;
        this.totalElements = p.totalElements;
        this.totalPages = p.totalPages;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadVendors();
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.editMode = false;
    this.showModal = true;
    this.error = '';
  }

  openEdit(v: Vendor): void {
    this.form = { ...v };
    this.editMode = true;
    this.showModal = true;
    this.error = '';
  }

  closeModal(): void {
    this.showModal = false;
  }

  save(): void {
    this.saving = true;
    this.error = '';

    const obs = this.editMode
      ? this.businessService.updateVendor(this.form.id, this.form)
      : this.businessService.createVendor(this.form);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.closeModal();
        this.loadVendors();
      },
      error: err => {
        this.error = err.error?.message || 'Error saving vendor';
        this.saving = false;
      }
    });
  }

  confirmDelete(id: number): void {
    this.deleteConfirmId = id;
  }

  cancelDelete(): void {
    this.deleteConfirmId = null;
  }

  deleteVendor(): void {
    if (!this.deleteConfirmId) return;
    this.businessService.deleteVendor(this.deleteConfirmId).subscribe({
      next: () => {
        this.deleteConfirmId = null;
        this.loadVendors();
      },
      error: err => alert(err.error?.message || 'Error deleting vendor')
    });
  }

  private emptyForm() {
    return {
      name: '',
      contactPerson: '',
      contactEmail: '',
      contactPhone: '',
      address: '',
      vendorType: 'PROCUREMENT',
      gstNumber: ''
    };
  }
}
