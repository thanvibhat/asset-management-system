import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MasterService } from '../../services/master.service';
import { AssetService } from '../../services/asset.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {
  activeTab: 'locations' | 'manufacturers' | 'roles' = 'locations';
  loading = false;
  saving = false;
  error = '';

  // Tab 1: Locations
  locations: any[] = [];
  locTotalElements = 0;
  locTotalPages = 0;
  locCurrentPage = 0;
  locPageSize = 10;
  showLocationModal = false;
  editLocationId: number | null = null;
  locationForm = { name: '', description: '' };

  // Tab 2: Manufacturers
  manufacturers: any[] = [];
  mfrTotalElements = 0;
  mfrTotalPages = 0;
  mfrCurrentPage = 0;
  mfrPageSize = 10;
  showManufacturerModal = false;
  editManufacturerId: number | null = null;
  manufacturerForm = { name: '', website: '' };


  constructor(
    private masterService: MasterService,
    private assetService: AssetService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.switchTab('locations');
  }

  switchTab(tab: 'locations' | 'manufacturers' | 'roles'): void {
    this.activeTab = tab;
    this.error = '';
    if (tab === 'locations') this.loadLocations();
    if (tab === 'manufacturers') this.loadManufacturers();
  }

  // --- Locations ---
  loadLocations(): void {
    this.loading = true;
    this.masterService.getLocations(this.locCurrentPage, this.locPageSize).subscribe({
      next: p => {
        this.locations = p.content;
        this.locTotalElements = p.totalElements;
        this.locTotalPages = p.totalPages;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  goToLocPage(page: number): void {
    this.locCurrentPage = page;
    this.loadLocations();
  }

  locPages(): number[] {
    return Array.from({ length: this.locTotalPages }, (_, i) => i);
  }

  openLocationModal(loc?: any): void {
    if (loc) {
      this.editLocationId = loc.id;
      this.locationForm = { name: loc.name, description: loc.description };
    } else {
      this.editLocationId = null;
      this.locationForm = { name: '', description: '' };
    }
    this.showLocationModal = true;
    this.error = '';
  }

  saveLocation(): void {
    this.saving = true;
    const obs = this.editLocationId
      ? this.masterService.updateLocation(this.editLocationId, this.locationForm)
      : this.masterService.createLocation(this.locationForm);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showLocationModal = false;
        this.loadLocations();
      },
      error: err => {
        this.error = err.error?.message || 'Error saving location';
        this.saving = false;
      }
    });
  }

  deleteLocation(id: number): void {
    if (confirm('Are you sure you want to delete this location?')) {
      this.masterService.deleteLocation(id).subscribe(() => this.loadLocations());
    }
  }

  // --- Manufacturers ---
  loadManufacturers(): void {
    this.loading = true;
    this.masterService.getManufacturers(this.mfrCurrentPage, this.mfrPageSize).subscribe({
      next: p => {
        this.manufacturers = p.content;
        this.mfrTotalElements = p.totalElements;
        this.mfrTotalPages = p.totalPages;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  goToMfrPage(page: number): void {
    this.mfrCurrentPage = page;
    this.loadManufacturers();
  }

  mfrPages(): number[] {
    return Array.from({ length: this.mfrTotalPages }, (_, i) => i);
  }

  openManufacturerModal(m?: any): void {
    if (m) {
      this.editManufacturerId = m.id;
      this.manufacturerForm = { name: m.name, website: m.website };
    } else {
      this.editManufacturerId = null;
      this.manufacturerForm = { name: '', website: '' };
    }
    this.showManufacturerModal = true;
    this.error = '';
  }

  saveManufacturer(): void {
    this.saving = true;
    const obs = this.editManufacturerId
      ? this.masterService.updateManufacturer(this.editManufacturerId, this.manufacturerForm)
      : this.masterService.createManufacturer(this.manufacturerForm);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showManufacturerModal = false;
        this.loadManufacturers();
      },
      error: err => {
        this.error = err.error?.message || 'Error saving manufacturer';
        this.saving = false;
      }
    });
  }

  deleteManufacturer(id: number): void {
    if (confirm('Are you sure you want to delete this manufacturer?')) {
      this.masterService.deleteManufacturer(id).subscribe(() => this.loadManufacturers());
    }
  }

}
