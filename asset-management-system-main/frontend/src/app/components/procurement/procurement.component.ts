import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BusinessService } from '../../services/business.service';
import { Procurement, Vendor } from '../../models/business.model';

@Component({
  selector: 'app-procurement',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './procurement.component.html',
  styleUrls: ['./procurement.component.css']
})
export class ProcurementComponent implements OnInit {
  procurements: Procurement[] = [];
  vendors: Vendor[] = [];
  loading = true;
  
  showModal = false;
  editMode = false;
  saving = false;
  error = '';
  deleteConfirmId: number | null = null;

  form: any = this.emptyForm();
  statuses = ['ORDERED', 'SHIPPED', 'RECEIVED', 'CANCELLED'];

  constructor(private businessService: BusinessService) {}

  ngOnInit(): void {
    this.loadVendors();
    this.loadProcurements();
  }

  loadProcurements(): void {
    this.loading = true;
    this.businessService.getProcurements().subscribe({
      next: data => {
        this.procurements = data;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  loadVendors(): void {
    this.businessService.getVendors().subscribe(data => this.vendors = data);
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.editMode = false;
    this.showModal = true;
    this.error = '';
  }

  openEdit(p: Procurement): void {
    this.form = { 
      ...p, 
      vendorId: p.vendor?.id,
      orderDate: p.orderDate ? p.orderDate.split('T')[0] : ''
    };
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

    const payload = {
      ...this.form,
      vendorId: +this.form.vendorId
    };

    const obs = this.editMode
      ? this.businessService.updateProcurement(this.form.id, payload)
      : this.businessService.createProcurement(payload);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.closeModal();
        this.loadProcurements();
      },
      error: err => {
        this.error = err.error?.message || 'Error saving procurement';
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

  deleteProcurement(): void {
    if (!this.deleteConfirmId) return;
    this.businessService.deleteProcurement(this.deleteConfirmId).subscribe({
      next: () => {
        this.deleteConfirmId = null;
        this.loadProcurements();
      },
      error: err => alert(err.error?.message || 'Error deleting procurement')
    });
  }

  private emptyForm() {
    return {
      poNumber: '',
      vendorId: null,
      orderDate: new Date().toISOString().split('T')[0],
      invoiceNumber: '',
      quantity: 1,
      totalCost: 0,
      status: 'ORDERED'
    };
  }
}
